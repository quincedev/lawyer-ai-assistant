package com.quince.lawyeraiassistant.evaluation;

import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCandidate;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCase;
import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationResult;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalEvaluationDatasetScoringTest {

    @Test
    void shouldPassSyntheticAnswerContainingAllRequiredConcepts()
            throws Exception {

        DeterministicAgentQualityEvaluator evaluator = new DeterministicAgentQualityEvaluator();

        for (AgentEvaluationCase evaluationCase : loadCases()) {

            String answer = buildSyntheticAnswer(
                    evaluationCase);

            AgentEvaluationResult result = evaluator.evaluate(
                    evaluationCase,
                    candidate(answer));

            assertTrue(
                    result.passed(),
                    () -> "Case failed: "
                            + evaluationCase.caseId()
                            + ", score="
                            + result.totalScore()
                            + ", missing="
                            + result.missingConcepts());

            assertEquals(
                    1.0D,
                    result.conceptCoverage(),
                    0.000001D);
        }
    }

    private String buildSyntheticAnswer(
            AgentEvaluationCase evaluationCase) {

        String concepts = String.join(
                "；",
                evaluationCase.requiredConcepts());

        String answer = "本案分析需要涵盖以下法律要点："
                + concepts
                + "。";

        if (answer.length() < evaluationCase.minimumAnswerLength()) {

            answer += "需要结合劳动合同、工资记录、"
                    + "解除通知和其他证据综合判断。"
                            .repeat(
                                    evaluationCase.minimumAnswerLength()
                                            / 20
                                            + 2);
        }

        return answer;
    }

    private AgentEvaluationCandidate candidate(
            String answer) {

        return new AgentEvaluationCandidate(
                answer,
                AgentStatus.FINISHED,
                new AgentPerformanceSnapshot(
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

                        10_000,
                        2,
                        0));
    }

    private List<AgentEvaluationCase> loadCases()
            throws Exception {

        try (InputStream inputStream = getClass().getResourceAsStream(
                "/evaluation/legal-evaluation-dataset.json")) {

            assertNotNull(inputStream);

            AgentEvaluationCase[] values = new ObjectMapper().readValue(
                    inputStream,
                    AgentEvaluationCase[].class);

            return List.copyOf(
                    Arrays.asList(values));
        }
    }
}