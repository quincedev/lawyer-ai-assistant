package com.quince.lawyeraiassistant.evaluation.metrics;

import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationResult;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MicrometerAgentEvaluationMetrics
        implements AgentEvaluationMetrics {

    private static final String EVALUATION_TYPE = "deterministic";

    private final Counter passedEvaluations;

    private final Counter failedEvaluations;

    private final DistributionSummary evaluationScore;

    public MicrometerAgentEvaluationMetrics(
            MeterRegistry meterRegistry) {

        Objects.requireNonNull(
                meterRegistry,
                "meterRegistry must not be null");

        this.passedEvaluations = Counter.builder(
                "agent.evaluations")
                .description(
                        "Total Agent quality evaluations")
                .tag(
                        "evaluation_type",
                        EVALUATION_TYPE)
                .tag(
                        "result",
                        "pass")
                .register(
                        meterRegistry);

        this.failedEvaluations = Counter.builder(
                "agent.evaluations")
                .description(
                        "Total Agent quality evaluations")
                .tag(
                        "evaluation_type",
                        EVALUATION_TYPE)
                .tag(
                        "result",
                        "fail")
                .register(
                        meterRegistry);

        /*
         * 不设置 baseUnit。
         *
         * 稳定 Prometheus 名称：
         * agent_evaluation_score_count
         * agent_evaluation_score_sum
         * agent_evaluation_score_max
         */
        this.evaluationScore = DistributionSummary.builder(
                "agent.evaluation.score")
                .description(
                        "Agent quality evaluation score from 0 to 100")
                .tag(
                        "evaluation_type",
                        EVALUATION_TYPE)
                .register(
                        meterRegistry);
    }

    @Override
    public void record(
            AgentEvaluationResult result) {

        Objects.requireNonNull(
                result,
                "result must not be null");

        evaluationScore.record(
                normalizeScore(
                        result.totalScore()));

        if (result.passed()) {

            passedEvaluations.increment();

        } else {

            failedEvaluations.increment();
        }
    }

    private double normalizeScore(
            double score) {

        if (Double.isNaN(score)
                || Double.isInfinite(score)) {

            return 0.0D;
        }

        return Math.max(
                0.0D,
                Math.min(
                        100.0D,
                        score));
    }
}