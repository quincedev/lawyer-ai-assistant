package com.quince.lawyeraiassistant.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * AI Cache Key 统一生成器。
 *
 * <p>
 * Cache Key 不直接包含用户 Query / Tool Arguments 原文，
 * 而是：
 * </p>
 *
 * <pre>
 * scope
 * + tenant
 * + input
 * + configuration
 * + knowledge version
 *       ↓
 * canonical representation
 *       ↓
 * SHA-256
 * </pre>
 */
@Component
public class CacheKeyFactory {

    private static final String SHARED_SCOPE_ID = "shared";

    private final ObjectMapper objectMapper;

    public CacheKeyFactory(
            ObjectMapper objectMapper) {

        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper must not be null");
    }

    public String retrievalKey(
            CacheScope scope,
            String tenantId,
            String query,
            String knowledgeVersion,
            int topK,
            double similarityThreshold) {

        Objects.requireNonNull(
                scope,
                "CacheScope must not be null");

        String normalizedQuery = normalizeRequiredText(
                query,
                "query");

        String normalizedVersion = normalizeRequiredText(
                knowledgeVersion,
                "knowledgeVersion");

        String rawKey = frame(
                scope.name(),
                resolveScopeId(
                        scope,
                        tenantId),
                normalizedQuery,
                normalizedVersion,
                Integer.toString(
                        topK),
                Double.toString(
                        similarityThreshold));

        return "rag:v1:"
                + sha256(
                        rawKey);
    }

    public String toolKey(
            CacheScope scope,
            String tenantId,
            String toolName,
            Map<String, Object> arguments,
            String knowledgeVersion) {

        Objects.requireNonNull(
                scope,
                "CacheScope must not be null");

        String normalizedToolName = normalizeRequiredText(
                toolName,
                "toolName");

        String normalizedVersion = normalizeRequiredText(
                knowledgeVersion,
                "knowledgeVersion");

        String canonicalArguments = canonicalizeArguments(
                arguments);

        String rawKey = frame(
                scope.name(),
                resolveScopeId(
                        scope,
                        tenantId),
                normalizedToolName,
                canonicalArguments,
                normalizedVersion);

        return "tool:v1:"
                + sha256(
                        rawKey);
    }

    private String resolveScopeId(
            CacheScope scope,
            String tenantId) {

        if (scope == CacheScope.SHARED) {

            return SHARED_SCOPE_ID;
        }

        return normalizeRequiredText(
                tenantId,
                "tenantId");
    }

    private String canonicalizeArguments(
            Map<String, Object> arguments) {

        Map<String, Object> safeArguments = arguments == null
                ? Map.of()
                : arguments;

        Object canonical = canonicalizeValue(
                safeArguments);

        try {

            return objectMapper
                    .writeValueAsString(
                            canonical);

        } catch (JacksonException exception) {

            throw new IllegalArgumentException(
                    "Failed to canonicalize cache arguments",
                    exception);
        }
    }

    private Object canonicalizeValue(
            Object value) {

        if (value == null) {

            return null;
        }

        if (value instanceof Map<?, ?> map) {

            Map<String, Object> sorted = new TreeMap<>();

            map.forEach(
                    (key, item) -> sorted.put(
                            String.valueOf(
                                    key),
                            canonicalizeValue(
                                    item)));

            return sorted;
        }

        if (value instanceof Collection<?> collection) {

            List<Object> result = new ArrayList<>();

            for (Object item : collection) {

                result.add(
                        canonicalizeValue(
                                item));
            }

            return result;
        }

        return value;
    }

    private String normalizeRequiredText(
            String value,
            String fieldName) {

        Objects.requireNonNull(
                value,
                fieldName + " must not be null");

        String normalized = value.trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName
                            + " must not be blank");
        }

        return normalized;
    }

    private String sha256(
            String input) {

        try {

            MessageDigest digest = MessageDigest.getInstance(
                    "SHA-256");

            byte[] hash = digest.digest(
                    input.getBytes(
                            StandardCharsets.UTF_8));

            return toHex(
                    hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception);
        }
    }

    private String frame(
            String... values) {

        StringBuilder builder = new StringBuilder();

        for (String value : values) {

            builder.append(value.length())
                    .append(':')
                    .append(value);
        }

        return builder.toString();
    }

    private String toHex(
            byte[] bytes) {

        StringBuilder builder = new StringBuilder(
                bytes.length * 2);

        for (byte value : bytes) {

            builder.append(
                    String.format(
                            "%02x",
                            value));
        }

        return builder.toString();
    }
}
