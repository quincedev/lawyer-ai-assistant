package com.quince.lawyeraiassistant.evaluation;

import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCase;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalEvaluationDatasetContractTest {

    private static final String DATASET = "/evaluation/legal-evaluation-dataset.json";

    @Test
    void shouldLoadValidLegalEvaluationDataset()
            throws Exception {

        List<AgentEvaluationCase> cases = loadCases();

        assertTrue(
                cases.size() >= 5,
                "Dataset must contain at least five cases");

        Set<String> caseIds = new HashSet<>();

        for (AgentEvaluationCase evaluationCase : cases) {

            assertTrue(
                    caseIds.add(
                            evaluationCase.caseId()),
                    "Duplicate caseId: "
                            + evaluationCase.caseId());

            assertFalse(
                    evaluationCase.requiredConcepts()
                            .isEmpty(),
                    "requiredConcepts must not be empty: "
                            + evaluationCase.caseId());

            assertTrue(
                    evaluationCase.minimumScore() > 0);

            assertTrue(
                    evaluationCase.maximumCostUsd()
                            .signum() > 0);
        }
    }

    @Test
    void shouldConstructEveryDatasetCaseWithoutValidationFailure()
            throws Exception {

        List<AgentEvaluationCase> cases = loadCases();

        assertDoesNotThrow(
                () -> cases.forEach(
                        evaluationCase -> {

                            assertNotNull(
                                    evaluationCase.caseId());

                            assertNotNull(
                                    evaluationCase.category());

                            assertNotNull(
                                    evaluationCase.question());
                        }));
    }

    private List<AgentEvaluationCase> loadCases()
            throws Exception {

        try (InputStream inputStream = getClass().getResourceAsStream(
                DATASET)) {

            assertNotNull(
                    inputStream,
                    "Dataset not found: "
                            + DATASET);

            ObjectMapper objectMapper = new ObjectMapper();

            AgentEvaluationCase[] values = objectMapper.readValue(
                    inputStream,
                    AgentEvaluationCase[].class);

            return List.copyOf(
                    Arrays.asList(values));
        }
    }
}