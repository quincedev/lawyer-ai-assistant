package com.quince.lawyeraiassistant.evaluation;

import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCandidate;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCase;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationResult;
import com.quince.lawyeraiassistant.evaluation.model.EvaluationDimensionScore;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class DeterministicAgentQualityEvaluator
                implements AgentQualityEvaluator {

        /*
         * 数据集可以使用 "|" 声明同一法律概念的多种等价表达。
         *
         * 示例：
         *
         * 经济补偿标准的二倍|经济补偿金的二倍|二倍经济补偿金|两倍经济补偿金
         *
         * 第一项是报告中显示的标准概念名称，其余项是允许匹配的别名。
         */
        private static final String ALTERNATIVE_SEPARATOR = "\\|";

        private static final double STATUS_WEIGHT = 10.0D;

        private static final double COMPLETENESS_WEIGHT = 20.0D;

        private static final double CONCEPT_WEIGHT = 50.0D;

        private static final double SAFETY_WEIGHT = 20.0D;

        @Override
        public AgentEvaluationResult evaluate(
                        AgentEvaluationCase evaluationCase,
                        AgentEvaluationCandidate candidate) {

                Objects.requireNonNull(
                                evaluationCase,
                                "evaluationCase must not be null");

                Objects.requireNonNull(
                                candidate,
                                "candidate must not be null");

                String answer = candidate.answer() == null
                                ? ""
                                : candidate.answer();

                String normalizedAnswer = normalize(answer);

                List<String> matchedConceptDefinitions = findMatchedDefinitions(
                                normalizedAnswer,
                                evaluationCase.requiredConcepts());

                List<String> matchedConcepts = matchedConceptDefinitions.stream()
                                .map(this::canonicalValue)
                                .toList();

                List<String> missingConcepts = evaluationCase.requiredConcepts()
                                .stream()
                                .filter(definition -> !matchedConceptDefinitions.contains(
                                                definition))
                                .map(this::canonicalValue)
                                .toList();

                List<String> matchedForbiddenDefinitions = findMatchedDefinitions(
                                normalizedAnswer,
                                evaluationCase.forbiddenClaims());

                List<String> matchedForbiddenClaims = matchedForbiddenDefinitions.stream()
                                .map(this::canonicalValue)
                                .toList();

                double conceptCoverage = calculateCoverage(
                                matchedConceptDefinitions.size(),
                                evaluationCase.requiredConcepts().size());

                double statusScore = candidate.status() == AgentStatus.FINISHED
                                ? STATUS_WEIGHT
                                : 0.0D;

                double completenessRatio = calculateCompletenessRatio(
                                answer.length(),
                                evaluationCase.minimumAnswerLength());

                double completenessScore = COMPLETENESS_WEIGHT * completenessRatio;

                double conceptScore = CONCEPT_WEIGHT * conceptCoverage;

                double safetyScore = matchedForbiddenClaims.isEmpty()
                                ? SAFETY_WEIGHT
                                : 0.0D;

                List<EvaluationDimensionScore> dimensions = new ArrayList<>();

                dimensions.add(
                                new EvaluationDimensionScore(
                                                "executionStatus",
                                                statusScore,
                                                STATUS_WEIGHT,
                                                candidate.status() == AgentStatus.FINISHED
                                                                ? "Agent execution finished"
                                                                : "Agent execution did not finish"));

                dimensions.add(
                                new EvaluationDimensionScore(
                                                "answerCompleteness",
                                                completenessScore,
                                                COMPLETENESS_WEIGHT,
                                                "answerLength="
                                                                + answer.length()
                                                                + ", minimumAnswerLength="
                                                                + evaluationCase.minimumAnswerLength()));

                dimensions.add(
                                new EvaluationDimensionScore(
                                                "requiredConceptCoverage",
                                                conceptScore,
                                                CONCEPT_WEIGHT,
                                                "matched="
                                                                + matchedConceptDefinitions.size()
                                                                + ", required="
                                                                + evaluationCase.requiredConcepts().size()
                                                                + ", coverage="
                                                                + conceptCoverage));

                dimensions.add(
                                new EvaluationDimensionScore(
                                                "forbiddenClaimSafety",
                                                safetyScore,
                                                SAFETY_WEIGHT,
                                                matchedForbiddenClaims.isEmpty()
                                                                ? "No forbidden claim matched"
                                                                : "Matched forbidden claims: "
                                                                                + matchedForbiddenClaims));

                double totalScore = normalizeScore(
                                statusScore
                                                + completenessScore
                                                + conceptScore
                                                + safetyScore);

                BigDecimal actualCostUsd = candidate.performance().llmCostUsd();

                BigDecimal maximumCostUsd = evaluationCase.maximumCostUsd();

                boolean withinCostBudget = actualCostUsd.compareTo(maximumCostUsd) <= 0;

                boolean passed = candidate.status() == AgentStatus.FINISHED
                                && totalScore >= evaluationCase.minimumScore()
                                && conceptCoverage >= evaluationCase.minimumConceptCoverage()
                                && matchedForbiddenClaims.isEmpty()
                                && withinCostBudget;

                return new AgentEvaluationResult(
                                evaluationCase.caseId(),
                                totalScore,
                                evaluationCase.minimumScore(),
                                passed,
                                conceptCoverage,
                                matchedConcepts,
                                missingConcepts,
                                matchedForbiddenClaims,
                                dimensions,
                                actualCostUsd,
                                maximumCostUsd,
                                withinCostBudget);
        }

        private List<String> findMatchedDefinitions(
                        String normalizedAnswer,
                        List<String> definitions) {

                return definitions.stream()
                                .filter(definition -> matchesAnyAlternative(
                                                normalizedAnswer,
                                                definition))
                                .toList();
        }

        private boolean matchesAnyAlternative(
                        String normalizedAnswer,
                        String definition) {

                if (definition == null
                                || definition.isBlank()) {

                        return false;
                }

                return List.of(
                                definition.split(
                                                ALTERNATIVE_SEPARATOR))
                                .stream()
                                .map(String::trim)
                                .filter(value -> !value.isBlank())
                                .map(this::normalize)
                                .anyMatch(normalizedAnswer::contains);
        }

        private String canonicalValue(
                        String definition) {

                if (definition == null
                                || definition.isBlank()) {

                        return "";
                }

                return definition.split(
                                ALTERNATIVE_SEPARATOR,
                                2)[0]
                                .trim();
        }

        private double calculateCoverage(
                        int matched,
                        int total) {

                if (total == 0) {
                        return 1.0D;
                }

                return (double) matched / total;
        }

        private double calculateCompletenessRatio(
                        int answerLength,
                        int minimumAnswerLength) {

                if (minimumAnswerLength == 0) {
                        return 1.0D;
                }

                return Math.min(
                                1.0D,
                                (double) answerLength
                                                / minimumAnswerLength);
        }

        private double normalizeScore(
                        double score) {

                return Math.max(
                                0.0D,
                                Math.min(
                                                100.0D,
                                                score));
        }

        private String normalize(
                        String value) {

                if (value == null) {
                        return "";
                }

                return value.trim()
                                .toLowerCase(Locale.ROOT)
                                .replaceAll("\\s+", "")
                                .replace("《", "")
                                .replace("》", "")
                                .replace("(", "(")
                                .replace(")", ")")
                                .replace("%", "%");
        }
}