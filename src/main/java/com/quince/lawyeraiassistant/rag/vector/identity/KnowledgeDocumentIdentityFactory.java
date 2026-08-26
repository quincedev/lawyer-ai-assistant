package com.quince.lawyeraiassistant.rag.vector.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;

import org.springframework.stereotype.Component;

@Component
@ConditionalOnRagEnabled
public class KnowledgeDocumentIdentityFactory {

        public String sourceDocumentId(
                        String sourceUri,
                        String sourceChecksum) {

                String normalizedSourceUri = requireText(
                                sourceUri,
                                "sourceUri");

                String normalizedSourceChecksum = requireText(
                                sourceChecksum,
                                "sourceChecksum");

                return "source:" + sha256(
                                normalizedSourceUri
                                                + "\n"
                                                + normalizedSourceChecksum);
        }

        public String parentDocumentId(
                        String sourceDocumentId,
                        int pageNumber,
                        String contentChecksum) {

                String normalizedSourceDocumentId = requireText(
                                sourceDocumentId,
                                "sourceDocumentId");

                String normalizedContentChecksum = requireText(
                                contentChecksum,
                                "contentChecksum");

                if (pageNumber < 1) {
                        throw new IllegalArgumentException(
                                        "pageNumber must be greater than zero");
                }

                return "parent:" + sha256(
                                normalizedSourceDocumentId
                                                + "\n"
                                                + pageNumber
                                                + "\n"
                                                + normalizedContentChecksum);
        }

        public String chunkId(
                        String parentDocumentId,
                        int chunkIndex,
                        String contentChecksum) {

                String normalizedParentDocumentId = requireText(
                                parentDocumentId,
                                "parentDocumentId");

                String normalizedContentChecksum = requireText(
                                contentChecksum,
                                "contentChecksum");

                if (chunkIndex < 0) {
                        throw new IllegalArgumentException(
                                        "chunkIndex must not be negative");
                }

                return "chunk:" + sha256(
                                normalizedParentDocumentId
                                                + "\n"
                                                + chunkIndex
                                                + "\n"
                                                + normalizedContentChecksum);
        }

        public String checksum(
                        byte[] content) {

                if (content == null) {
                        throw new IllegalArgumentException(
                                        "Content must not be null");
                }

                if (content.length == 0) {
                        throw new IllegalArgumentException(
                                        "Content must not be empty");
                }

                return sha256(content);
        }

        public String checksum(
                        String content) {

                if (content == null) {
                        throw new IllegalArgumentException(
                                        "Content must not be null");
                }

                String normalized = normalizeContent(
                                content);

                if (normalized.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Content must not be blank");
                }

                return sha256(
                                normalized.getBytes(
                                                StandardCharsets.UTF_8));
        }

        private String normalizeContent(
                        String content) {

                return content
                                .replace("\r\n", "\n")
                                .replace('\r', '\n')
                                .strip();
        }

        private String requireText(
                        String value,
                        String fieldName) {

                if (value == null || value.isBlank()) {
                        throw new IllegalArgumentException(
                                        fieldName + " must not be blank");
                }

                return value.trim();
        }

        private String sha256(
                        String value) {

                return sha256(
                                value.getBytes(
                                                StandardCharsets.UTF_8));
        }

        private String sha256(
                        byte[] value) {

                try {
                        MessageDigest digest = MessageDigest.getInstance(
                                        "SHA-256");

                        return HexFormat.of()
                                        .formatHex(
                                                        digest.digest(value));
                } catch (NoSuchAlgorithmException exception) {
                        throw new IllegalStateException(
                                        "SHA-256 is not available",
                                        exception);
                }
        }
}
