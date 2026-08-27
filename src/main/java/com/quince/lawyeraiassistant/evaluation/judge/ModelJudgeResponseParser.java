package com.quince.lawyeraiassistant.evaluation.judge;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

@Component
public class ModelJudgeResponseParser {

    private final ObjectMapper objectMapper;

    public ModelJudgeResponseParser(
            ObjectMapper objectMapper) {

        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper must not be null");
    }

    public ModelJudgeResponsePayload parse(
            String responseText) {

        String json = extractJson(responseText);

        try {

            return objectMapper.readValue(
                    json,
                    ModelJudgeResponsePayload.class);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to parse model judge response",
                    exception);
        }
    }

    private String extractJson(
            String responseText) {

        if (responseText == null
                || responseText.isBlank()) {

            throw new IllegalStateException(
                    "Model judge returned an empty response");
        }

        String normalized = responseText.trim();

        if (normalized.startsWith("```")) {

            normalized = normalized.replaceFirst(
                    "^```(?:json)?\\s*",
                    "");

            normalized = normalized.replaceFirst(
                    "\\s*```$",
                    "");
        }

        int firstBrace = normalized.indexOf('{');

        int lastBrace = normalized.lastIndexOf('}');

        if (firstBrace < 0
                || lastBrace < firstBrace) {

            throw new IllegalStateException(
                    "Model judge response does not contain a JSON object");
        }

        return normalized.substring(
                firstBrace,
                lastBrace + 1);
    }
}