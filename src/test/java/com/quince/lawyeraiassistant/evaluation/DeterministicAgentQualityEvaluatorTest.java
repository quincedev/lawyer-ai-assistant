package com.quince.lawyeraiassistant.evaluation;

import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCandidate;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCase;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicAgentQualityEvaluatorTest {

        private DeterministicAgentQualityEvaluator evaluator;

        @BeforeEach
        void setUp() {

                evaluator = new DeterministicAgentQualityEvaluator();
        }

        @Test
        void shouldPassCompleteSafeAnswerWithinCostBudget() {

                AgentEvaluationResult result = evaluator.evaluate(
                                evaluationCase(),
                                candidate(
                                                completeAnswer(),
                                                AgentStatus.FINISHED,
                                                20_000));

                assertTrue(result.passed());

                assertEquals(
                                100.0D,
                                result.totalScore(),
                                0.000001D);

                assertEquals(
                                1.0D,
                                result.conceptCoverage(),
                                0.000001D);

                assertTrue(
                                result.missingConcepts()
                                                .isEmpty());

                assertTrue(
                                result.matchedForbiddenClaims()
                                                .isEmpty());

                assertTrue(
                                result.withinCostBudget());
        }

        @Test
        void shouldFailWhenRequiredConceptCoverageIsTooLow() {

                AgentEvaluationResult result = evaluator.evaluate(
                                evaluationCase(),
                                candidate(
                                                "违法解除需要承担相应责任。"
                                                                + "补充说明。".repeat(100),
                                                AgentStatus.FINISHED,
                                                10_000));

                assertFalse(result.passed());

                assertTrue(
                                result.conceptCoverage() < evaluationCase()
                                                .minimumConceptCoverage());

                assertFalse(
                                result.missingConcepts()
                                                .isEmpty());
        }

        @Test
        void shouldFailWhenForbiddenClaimIsMatched() {

                String answer = completeAnswer()
                                + "但是违法解除不需要赔偿。";

                AgentEvaluationResult result = evaluator.evaluate(
                                evaluationCase(),
                                candidate(
                                                answer,
                                                AgentStatus.FINISHED,
                                                10_000));

                assertFalse(result.passed());

                assertEquals(
                                List.of("违法解除不需要赔偿"),
                                result.matchedForbiddenClaims());
        }

        @Test
        void shouldFailWhenAnswerIsTooShort() {

                AgentEvaluationCase evaluationCase = new AgentEvaluationCase(
                                "short-answer-case",
                                "labor-law",
                                "测试问题",
                                List.of("赔偿金"),
                                List.of(),
                                500,
                                1.0D,
                                95.0D,
                                new BigDecimal("0.05"));

                AgentEvaluationResult result = evaluator.evaluate(
                                evaluationCase,
                                candidate(
                                                "应当支付赔偿金。",
                                                AgentStatus.FINISHED,
                                                1_000));

                assertFalse(result.passed());

                assertTrue(
                                result.totalScore() < 95.0D);
        }

        @Test
        void shouldFailWhenExecutionDidNotFinish() {

                AgentEvaluationResult result = evaluator.evaluate(
                                evaluationCase(),
                                candidate(
                                                completeAnswer(),
                                                AgentStatus.FAILED,
                                                10_000));

                assertFalse(result.passed());

                assertEquals(
                                0.0D,
                                dimensionScore(
                                                result,
                                                "executionStatus"),
                                0.000001D);
        }

        @Test
        void shouldFailWhenExecutionExceedsCostBudget() {

                AgentEvaluationResult result = evaluator.evaluate(
                                evaluationCase(),
                                candidate(
                                                completeAnswer(),
                                                AgentStatus.FINISHED,
                                                60_000));

                assertFalse(result.passed());

                assertFalse(
                                result.withinCostBudget());

                assertEquals(
                                0,
                                new BigDecimal("0.06")
                                                .compareTo(
                                                                result.actualCostUsd()));

                assertEquals(
                                new BigDecimal("0.05"),
                                result.maximumCostUsd());
        }

        @Test
        void shouldNormalizeWhitespaceDuringConceptMatching() {

                String answer = """
                                用人单位构成违法解除。
                                劳动者可以要求支付赔 偿 金。
                                赔偿标准为经济补偿标准的 二 倍。
                                应结合工作 年限计算。
                                依据劳动合同法第 四 十 八 条，
                                以及劳动合同法第 八 十 七 条。
                                """
                                + "补充分析。".repeat(80);

                AgentEvaluationResult result = evaluator.evaluate(
                                evaluationCase(),
                                candidate(
                                                answer,
                                                AgentStatus.FINISHED,
                                                10_000));

                assertEquals(
                                1.0D,
                                result.conceptCoverage(),
                                0.000001D);
        }

        @Test
        void shouldMatchEquivalentConceptExpressions() {

                AgentEvaluationCase evaluationCase = new AgentEvaluationCase(
                                "concept-alternative-case",
                                "labor-law",
                                "测试问题",
                                List.of(
                                                "经济补偿标准的二倍|两倍经济补偿金",
                                                "劳动合同法第八十七条|劳动合同法第87条",
                                                "百分之一百五十|150%|1.5倍"),
                                List.of(),
                                0,
                                1.0D,
                                100.0D,
                                new BigDecimal("0.05"));

                String answer = """
                                用人单位应当支付两倍经济补偿金。
                                其法律依据包括《劳动合同法》第87条。
                                工作日延长工作时间应支付150%的工资报酬。
                                """;

                AgentEvaluationResult result = evaluator.evaluate(
                                evaluationCase,
                                candidate(
                                                answer,
                                                AgentStatus.FINISHED,
                                                10_000));

                assertEquals(
                                1.0D,
                                result.conceptCoverage(),
                                0.000001D);

                assertTrue(
                                result.missingConcepts().isEmpty());

                assertTrue(result.passed());
        }

        private AgentEvaluationCase evaluationCase() {

                return new AgentEvaluationCase(
                                "labor-illegal-termination-001",
                                "labor-law",
                                "用人单位违法解除劳动合同需要承担什么责任？",
                                List.of(
                                                "违法解除",
                                                "赔偿金",
                                                "经济补偿标准的二倍",
                                                "工作年限",
                                                "劳动合同法第四十八条",
                                                "劳动合同法第八十七条"),
                                List.of(
                                                "违法解除不需要赔偿"),
                                200,
                                0.66D,
                                75.0D,
                                new BigDecimal("0.05"));
        }

        private AgentEvaluationCandidate candidate(
                        String answer,
                        AgentStatus status,
                        long costMicros) {

                return new AgentEvaluationCandidate(
                                answer,
                                status,
                                snapshot(costMicros));
        }

        private AgentPerformanceSnapshot snapshot(
                        long costMicros) {

                return new AgentPerformanceSnapshot(
                                10_000,

                                2,
                                9_000,

                                2_000,
                                1_000,
                                3_000,
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

                                costMicros,
                                2,
                                0);
        }

        private String completeAnswer() {

                return """
                                用人单位无法定理由解除劳动合同的，
                                构成违法解除。劳动者可以要求继续履行，
                                或者要求用人单位支付赔偿金。
                                赔偿金按照经济补偿标准的二倍计算，
                                具体需要结合劳动者的工作年限和月工资。
                                其主要法律依据包括劳动合同法第四十八条
                                以及劳动合同法第八十七条。
                                """
                                + "同时应当保留解除通知、工资记录、"
                                + "劳动合同等证据。".repeat(20);
        }

        private double dimensionScore(
                        AgentEvaluationResult result,
                        String dimension) {

                return result.dimensions()
                                .stream()
                                .filter(value -> value.dimension()
                                                .equals(dimension))
                                .findFirst()
                                .orElseThrow()
                                .score();
        }
}