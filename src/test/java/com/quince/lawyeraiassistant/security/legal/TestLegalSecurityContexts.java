package com.quince.lawyeraiassistant.security.legal;

public final class TestLegalSecurityContexts {

    private TestLegalSecurityContexts() {
    }

    public static LegalSecurityContext toolResult() {

        return LegalSecurityContext.of(
                SecuritySource.TOOL_RESULT,
                SecurityTrustLevel.UNTRUSTED);
    }

    public static LegalSecurityContext mcpResult() {

        return LegalSecurityContext.of(
                SecuritySource.MCP_RESULT,
                SecurityTrustLevel.UNTRUSTED);
    }

    public static LegalSecurityContext runtimeDerived() {

        return LegalSecurityContext.of(
                SecuritySource.RUNTIME,
                SecurityTrustLevel.DERIVED);
    }
}