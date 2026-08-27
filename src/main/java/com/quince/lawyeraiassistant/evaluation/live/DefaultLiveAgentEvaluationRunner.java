package com.quince.lawyeraiassistant.evaluation.live;

import com.quince.lawyeraiassistant.agent.application.AgentApplicationService;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshotRecorder;
import com.quince.lawyeraiassistant.evaluation.AgentEvaluationService;
import com.quince.lawyeraiassistant.evaluation.judge.ModelJudgeEvaluator;
import com.quince.lawyeraiassistant.evaluation.judge.ModelJudgeProperties;
import com.quince.lawyeraiassistant.evaluation.judge.ModelJudgeRequest;
import com.quince.lawyeraiassistant.evaluation.judge.ModelJudgeResult;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCandidate;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCase;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Profile("!mcp-server")
@ConditionalOnProperty(prefix = "app.evaluation.live", name = "enabled", havingValue = "true")
public class DefaultLiveAgentEvaluationRunner
                implements LiveAgentEvaluationRunner {

        private static final Logger LOGGER = LoggerFactory.getLogger(
                        DefaultLiveAgentEvaluationRunner.class);

        private final AgentEvaluationDataset dataset;

        private final AgentApplicationService agentApplicationService;

        private final AgentEvaluationService evaluationService;

        private final AgentPerformanceSnapshotRecorder snapshotRecorder;

        private final LiveEvaluationProperties properties;

        private final ReentrantLock executionLock = new ReentrantLock();

        private static final int ANSWER_PREVIEW_MAXIMUM_LENGTH = 4_000;

        private final ModelJudgeEvaluator modelJudgeEvaluator;

        private final ModelJudgeProperties modelJudgeProperties;

        public DefaultLiveAgentEvaluationRunner(
                        AgentEvaluationDataset dataset,
                        AgentApplicationService agentApplicationService,
                        AgentEvaluationService evaluationService,
                        AgentPerformanceSnapshotRecorder snapshotRecorder,
                        LiveEvaluationProperties properties,
                        ModelJudgeEvaluator modelJudgeEvaluator,
                        ModelJudgeProperties modelJudgeProperties) {

                this.dataset = Objects.requireNonNull(
                                dataset,
                                "dataset must not be null");

                this.agentApplicationService = Objects.requireNonNull(
                                agentApplicationService,
                                "agentApplicationService must not be null");

                this.evaluationService = Objects.requireNonNull(
                                evaluationService,
                                "evaluationService must not be null");

                this.snapshotRecorder = Objects.requireNonNull(
                                snapshotRecorder,
                                "snapshotRecorder must not be null");

                this.properties = Objects.requireNonNull(
                                properties,
                                "properties must not be null");

                this.modelJudgeEvaluator = Objects.requireNonNull(
                                modelJudgeEvaluator,
                                "modelJudgeEvaluator must not be null");

                this.modelJudgeProperties = Objects.requireNonNull(
                                modelJudgeProperties,
                                "modelJudgeProperties must not be null");
        }

        @Override
        public LiveEvaluationReport runAll() {

                List<AgentEvaluationCase> cases = dataset.load()
                                .stream()
                                .limit(
                                                properties.getMaximumCases())
                                .toList();

                return runCases(cases);
        }

        @Override
        public LiveEvaluationReport runCase(
                        String caseId) {

                if (caseId == null
                                || caseId.isBlank()) {

                        throw new IllegalArgumentException(
                                        "caseId must not be blank");
                }

                AgentEvaluationCase evaluationCase = dataset.findById(caseId)
                                .orElseThrow(
                                                () -> new IllegalArgumentException(
                                                                "Evaluation case not found: "
                                                                                + caseId));

                return runCases(
                                List.of(evaluationCase));
        }

        private LiveEvaluationReport runCases(
                        List<AgentEvaluationCase> cases) {

                if (!executionLock.tryLock()) {

                        throw new IllegalStateException(
                                        "Another live evaluation is already running");
                }

                String runId = UUID.randomUUID().toString();

                Instant startedAt = Instant.now();

                try {

                        List<LiveEvaluationCaseReport> reports = new ArrayList<>();

                        for (AgentEvaluationCase evaluationCase : cases) {

                                reports.add(
                                                runSingleCase(
                                                                evaluationCase));
                        }

                        return aggregate(
                                        runId,
                                        startedAt,
                                        Instant.now(),
                                        reports);

                } finally {

                        executionLock.unlock();
                }
        }

        private LiveEvaluationCaseReport runSingleCase(
                        AgentEvaluationCase evaluationCase) {

                snapshotRecorder.clearCurrentThread();

                try {

                        AgentContext context = agentApplicationService.execute(
                                        evaluationCase.question());

                        AgentPerformanceSnapshot snapshot = snapshotRecorder
                                        .requireCurrentThreadLatest();

                        String answer = context.getFinalAnswer();

                        AgentEvaluationResult result = evaluationService.evaluate(
                                        evaluationCase,
                                        new AgentEvaluationCandidate(
                                                        answer,
                                                        context.getStatus(),
                                                        snapshot));

                        ModelJudgeResult modelJudgeResult = evaluateWithModelJudge(
                                        evaluationCase,
                                        answer);

                        return successReport(
                                        evaluationCase,
                                        context,
                                        snapshot,
                                        result,
                                        modelJudgeResult);

                } catch (RuntimeException exception) {

                        LOGGER.error(
                                        "Live Agent evaluation case failed. caseId={}, category={}, errorType={}",
                                        evaluationCase.caseId(),
                                        evaluationCase.category(),
                                        exception.getClass()
                                                        .getSimpleName(),
                                        exception);

                        AgentPerformanceSnapshot snapshot = snapshotRecorder
                                        .currentThreadLatest()
                                        .orElseGet(
                                                        this::emptySnapshot);

                        /*
                         * Agent 执行失败时仍生成确定性 FAIL，
                         * 但没有有效答案，所以不调用 Judge。
                         */
                        AgentEvaluationResult result = evaluationService.evaluate(
                                        evaluationCase,
                                        new AgentEvaluationCandidate(
                                                        "",
                                                        AgentStatus.FAILED,
                                                        snapshot));

                        return failureReport(
                                        evaluationCase,
                                        snapshot,
                                        result,
                                        exception);

                } finally {

                        snapshotRecorder.clearCurrentThread();
                }
        }

        private LiveEvaluationCaseReport successReport(
                        AgentEvaluationCase evaluationCase,
                        AgentContext context,
                        AgentPerformanceSnapshot snapshot,
                        AgentEvaluationResult result,
                        ModelJudgeResult modelJudgeResult) {

                boolean compositePassed = determineCompositePassed(
                                result,
                                modelJudgeResult);

                return new LiveEvaluationCaseReport(
                                evaluationCase.caseId(),
                                evaluationCase.category(),
                                context.getStatus().name(),
                                result.totalScore(),
                                result.minimumScore(),
                                result.passed(),
                                compositePassed,
                                result.conceptCoverage(),
                                result.missingConcepts(),
                                result.matchedForbiddenClaims(),
                                result.withinCostBudget(),
                                snapshot.totalDurationMs(),
                                snapshot.llmCalls(),
                                snapshot.promptTokens(),
                                snapshot.completionTokens(),
                                snapshot.totalTokens(),
                                snapshot.usageMissingCalls(),
                                snapshot.pricedLlmCalls(),
                                snapshot.pricingMissingCalls(),
                                snapshot.llmCostUsd(),
                                createAnswerPreview(
                                                context.getFinalAnswer()),
                                modelJudgeResult,
                                "");
        }

        private LiveEvaluationCaseReport failureReport(
                        AgentEvaluationCase evaluationCase,
                        AgentPerformanceSnapshot snapshot,
                        AgentEvaluationResult result,
                        RuntimeException exception) {

                return new LiveEvaluationCaseReport(
                                evaluationCase.caseId(),
                                evaluationCase.category(),
                                AgentStatus.FAILED.name(),
                                result.totalScore(),
                                result.minimumScore(),
                                result.passed(),
                                false,
                                result.conceptCoverage(),
                                result.missingConcepts(),
                                result.matchedForbiddenClaims(),
                                result.withinCostBudget(),
                                snapshot.totalDurationMs(),
                                snapshot.llmCalls(),
                                snapshot.promptTokens(),
                                snapshot.completionTokens(),
                                snapshot.totalTokens(),
                                snapshot.usageMissingCalls(),
                                snapshot.pricedLlmCalls(),
                                snapshot.pricingMissingCalls(),
                                snapshot.llmCostUsd(),
                                "",
                                ModelJudgeResult.skipped(),
                                exception.getClass()
                                                .getSimpleName());
        }

        private LiveEvaluationReport aggregate(
                        String runId,
                        Instant startedAt,
                        Instant completedAt,
                        List<LiveEvaluationCaseReport> reports) {

                int totalCases = reports.size();

                int passedCases = (int) reports.stream()
                                .filter(
                                                LiveEvaluationCaseReport::passed)
                                .count();

                int failedCases = totalCases - passedCases;

                double passRate = totalCases == 0
                                ? 0.0D
                                : (double) passedCases
                                                / totalCases;

                double averageScore = reports.stream()
                                .mapToDouble(
                                                LiveEvaluationCaseReport::score)
                                .average()
                                .orElse(0.0D);

                long totalPromptTokens = reports.stream()
                                .mapToLong(
                                                LiveEvaluationCaseReport::promptTokens)
                                .sum();

                long totalCompletionTokens = reports.stream()
                                .mapToLong(
                                                LiveEvaluationCaseReport::completionTokens)
                                .sum();

                long totalTokens = reports.stream()
                                .mapToLong(
                                                LiveEvaluationCaseReport::totalTokens)
                                .sum();

                BigDecimal totalCostUsd = reports.stream()
                                .map(
                                                LiveEvaluationCaseReport::costUsd)
                                .reduce(
                                                BigDecimal.ZERO,
                                                BigDecimal::add);

                return new LiveEvaluationReport(
                                runId,
                                startedAt,
                                completedAt,
                                totalCases,
                                passedCases,
                                failedCases,
                                passRate,
                                averageScore,
                                totalPromptTokens,
                                totalCompletionTokens,
                                totalTokens,
                                totalCostUsd,
                                reports);
        }

        private AgentPerformanceSnapshot emptySnapshot() {

                return new AgentPerformanceSnapshot(
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0);
        }

        private String createAnswerPreview(
                        String answer) {

                if (answer == null
                                || answer.isBlank()) {

                        return "";
                }

                String normalized = answer.trim();

                if (normalized.length() <= ANSWER_PREVIEW_MAXIMUM_LENGTH) {

                        return normalized;
                }

                return normalized.substring(
                                0,
                                ANSWER_PREVIEW_MAXIMUM_LENGTH)
                                + "...";
        }

        private ModelJudgeResult evaluateWithModelJudge(
                        AgentEvaluationCase evaluationCase,
                        String answer) {

                try {

                        return modelJudgeEvaluator.evaluate(
                                        new ModelJudgeRequest(
                                                        evaluationCase.caseId(),
                                                        evaluationCase.category(),
                                                        evaluationCase.question(),
                                                        evaluationCase.requiredConcepts(),
                                                        evaluationCase.forbiddenClaims(),
                                                        answer));

                } catch (RuntimeException exception) {

                        return ModelJudgeResult.failed(
                                        exception.getClass()
                                                        .getSimpleName(),
                                        modelJudgeProperties.isFailOpen());
                }
        }

        private boolean determineCompositePassed(
                        AgentEvaluationResult deterministicResult,
                        ModelJudgeResult modelJudgeResult) {

                if (!deterministicResult.passed()) {
                        return false;
                }

                return switch (modelJudgeResult.status()) {

                        case DISABLED ->
                                true;

                        case COMPLETED,
                                        FAILED ->
                                modelJudgeResult.passed();

                        case SKIPPED ->
                                false;
                };
        }
}
