package com.quince.lawyeraiassistant.evaluation.judge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModelJudgeResponseParserTest {

    private ModelJudgeResponseParser parser;

    @BeforeEach
    void setUp() {

        parser = new ModelJudgeResponseParser(
                new ObjectMapper());
    }

    @Test
    void shouldParsePlainJsonResponse() {

        ModelJudgeResponsePayload result = parser.parse("""
                {
                  "overallScore": 92,
                  "legalCorrectnessScore": 95,
                  "completenessScore": 90,
                  "relevanceScore": 94,
                  "riskControlScore": 89,
                  "criticalRisk": false,
                  "reasons": [
                    "法律结论正确"
                  ],
                  "risks": []
                }
                """);

        assertEquals(
                92.0D,
                result.overallScore(),
                0.000001D);

        assertEquals(
                95.0D,
                result.legalCorrectnessScore(),
                0.000001D);

        assertFalse(
                result.criticalRisk());

        assertEquals(
                List.of("法律结论正确"),
                result.reasons());
    }

    @Test
    void shouldRemoveMarkdownCodeFence() {

        ModelJudgeResponsePayload result = parser.parse("""
                ```json
                {
                  "overallScore": 80,
                  "legalCorrectnessScore": 80,
                  "completenessScore": 80,
                  "relevanceScore": 80,
                  "riskControlScore": 80,
                  "criticalRisk": false,
                  "reasons": [],
                  "risks": []
                }
                ```
                """);

        assertEquals(
                80.0D,
                result.overallScore(),
                0.000001D);
    }
}