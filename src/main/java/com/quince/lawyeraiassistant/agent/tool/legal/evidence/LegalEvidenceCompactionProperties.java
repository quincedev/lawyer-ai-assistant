package com.quince.lawyeraiassistant.agent.tool.legal.evidence;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.agent.legal-evidence-compaction")
public class LegalEvidenceCompactionProperties {

    /**
     * 是否启用 Tool Result → Agent Observation 压缩。
     */
    private boolean enabled = true;

    /**
     * Observation 最终允许的最大字符数。
     *
     * 不建议顶到 Runtime Guardrail 上限，
     * 要留一定安全空间。
     */
    private int maxChars = 8000;

    /**
     * 每个证据片段最多保留多少字符。
     */
    private int maxSectionChars = 1800;

    /**
     * 最多保留多少个主要证据片段。
     */
    private int maxSections = 4;

    public boolean isEnabled() {

        return enabled;
    }

    public void setEnabled(
            boolean enabled) {

        this.enabled = enabled;
    }

    public int getMaxChars() {

        return maxChars;
    }

    public void setMaxChars(
            int maxChars) {

        this.maxChars = maxChars;
    }

    public int getMaxSectionChars() {

        return maxSectionChars;
    }

    public void setMaxSectionChars(
            int maxSectionChars) {

        this.maxSectionChars = maxSectionChars;
    }

    public int getMaxSections() {

        return maxSections;
    }

    public void setMaxSections(
            int maxSections) {

        this.maxSections = maxSections;
    }
}