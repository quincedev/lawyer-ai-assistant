package com.quince.lawyeraiassistant.agent.tool.legal.evidence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultLegalEvidenceCompactorTest {

    private LegalEvidenceCompactionProperties properties;

    private DefaultLegalEvidenceCompactor compactor;

    @BeforeEach
    void setUp() {

        properties = new LegalEvidenceCompactionProperties();
        compactor = new DefaultLegalEvidenceCompactor(properties);
    }

    @Test
    void shouldLeaveShortContentUnchanged() {

        String content = "Article 1 establishes the applicable rule.";

        assertEquals(content, compactor.compact(content));
    }

    @Test
    void shouldLimitLongContentToMaxChars() {

        properties.setMaxChars(100);

        String result = compactor.compact("evidence".repeat(100));

        assertTrue(result.length() <= properties.getMaxChars());
    }

    @Test
    void shouldLimitNumberOfSections() {

        properties.setMaxChars(30);
        properties.setMaxSectionChars(100);
        properties.setMaxSections(2);

        String result = compactor.compact(
                "section-one\n\nsection-two\n\nsection-three");

        assertTrue(result.contains("section-one"));
        assertTrue(result.contains("section-two"));
        assertFalse(result.contains("section-three"));
    }

    @Test
    void shouldLimitLargeSectionToMaxSectionChars() {

        properties.setMaxChars(100);
        properties.setMaxSectionChars(30);

        String result = compactor.compact("x".repeat(200));

        assertTrue(result.length() <= properties.getMaxSectionChars());
    }

    @Test
    void shouldLeaveBlankContentUnchanged() {

        String content = " \n\t ";

        assertEquals(content, compactor.compact(content));
    }

    @Test
    void shouldLeaveContentUnchangedWhenDisabled() {

        properties.setEnabled(false);
        String content = "  evidence\r\nwith original formatting  ";

        assertEquals(content, compactor.compact(content));
    }

    @Test
    void shouldPreserveMultipleDocumentsInsteadOfBlankLineSections() {

        LegalEvidenceCompactionProperties properties = new LegalEvidenceCompactionProperties();

        properties.setEnabled(true);
        properties.setMaxChars(8000);
        properties.setMaxSectionChars(1800);
        properties.setMaxSections(4);

        DefaultLegalEvidenceCompactor compactor = new DefaultLegalEvidenceCompactor(
                properties);

        String input = """
                有效检索问题：违法解除劳动合同法律责任
                检索文档数量：4

                参考资料 1
                来源：law-1.pdf
                页码：1
                内容：
                %s

                参考资料 2
                来源：law-2.pdf
                页码：2
                内容：
                %s

                参考资料 3
                来源：law-3.pdf
                页码：3
                内容：
                %s

                参考资料 4
                来源：law-4.pdf
                页码：4
                内容：
                %s
                """.formatted(
                "A".repeat(4000),
                "B".repeat(4000),
                "C".repeat(4000),
                "D".repeat(4000));

        String result = compactor.compact(
                input);

        assertTrue(
                result.length() <= 8000);

        assertTrue(
                result.contains(
                        "参考资料 1"));

        assertTrue(
                result.contains(
                        "参考资料 2"));

        assertTrue(
                result.contains(
                        "参考资料 3"));

        assertTrue(
                result.contains(
                        "参考资料 4"));
    }

    @Test
    void shouldNotOverCompactStructuredLegalEvidence() {

        String input = buildLargeStructuredEvidence();

        String result = compactor.compact(
                input);

        assertTrue(
                result.length() > 4000);

        assertTrue(
                result.length() <= 8000);
    }

    @Test
    void shouldFallbackToTruncationWhenDocumentStructureCannotBeParsed() {

        String input = "X".repeat(
                20000);

        String result = compactor.compact(
                input);

        assertTrue(
                result.length() <= 8000);

        assertTrue(
                result.contains(
                        "[Evidence truncated]"));
    }

    private String buildLargeStructuredEvidence() {

        StringBuilder evidence = new StringBuilder(
                "有效检索问题：违法解除劳动合同法律责任\n"
                        + "检索文档数量：4");

        for (int documentNumber = 1; documentNumber <= 4; documentNumber++) {

            evidence.append("\n\n")
                    .append("参考资料 ")
                    .append(documentNumber)
                    .append("\n来源：law-")
                    .append(documentNumber)
                    .append(".pdf\n页码：")
                    .append(documentNumber)
                    .append("\n内容：")
                    .append(String.valueOf((char) ('A' + documentNumber - 1))
                            .repeat(3000));
        }

        return evidence.toString();
    }
}
