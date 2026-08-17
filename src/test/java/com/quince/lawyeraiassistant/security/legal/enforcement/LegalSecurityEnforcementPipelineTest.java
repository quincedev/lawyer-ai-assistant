package com.quince.lawyeraiassistant.security.legal.enforcement;

import com.quince.lawyeraiassistant.security.SecurityTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SecurityTest
class LegalSecurityEnforcementPipelineTest {

    private LegalSecurityEnforcementPipeline pipeline;

    @BeforeEach
    void setUp() {

        pipeline = new LegalSecurityEnforcementPipeline();
    }

    @Test
    void shouldExposeSecurityStagesInExpectedOrder() {

        assertEquals(
                List.of(
                        LegalSecurityEnforcementStage.INPUT_GUARDRAIL,
                        LegalSecurityEnforcementStage.SECURITY_CONTEXT,
                        LegalSecurityEnforcementStage.SKILL_BOUNDARY,
                        LegalSecurityEnforcementStage.ACTION_SELECTION,
                        LegalSecurityEnforcementStage.TOOL_EXISTENCE,
                        LegalSecurityEnforcementStage.TOOL_SCOPE,
                        LegalSecurityEnforcementStage.TOOL_RISK,
                        LegalSecurityEnforcementStage.TOOL_AUTHORIZATION,
                        LegalSecurityEnforcementStage.MCP_SECURITY,
                        LegalSecurityEnforcementStage.TOOL_EXECUTION,
                        LegalSecurityEnforcementStage.EVIDENCE_TRUST,
                        LegalSecurityEnforcementStage.RESOURCE_PROTECTION,
                        LegalSecurityEnforcementStage.MODEL_VISIBLE_EVIDENCE,
                        LegalSecurityEnforcementStage.FINAL_ANSWER),
                pipeline.stages());
    }

    @Test
    void shouldAuthorizeToolBeforeToolExecution() {

        assertTrue(
                pipeline.occursBefore(
                        LegalSecurityEnforcementStage.TOOL_AUTHORIZATION,
                        LegalSecurityEnforcementStage.TOOL_EXECUTION));
    }

    @Test
    void shouldApplyMcpSecurityBeforeToolExecution() {

        assertTrue(
                pipeline.occursBefore(
                        LegalSecurityEnforcementStage.MCP_SECURITY,
                        LegalSecurityEnforcementStage.TOOL_EXECUTION));
    }

    @Test
    void shouldValidateEvidenceBeforeModelExposure() {

        assertTrue(
                pipeline.occursBefore(
                        LegalSecurityEnforcementStage.EVIDENCE_TRUST,
                        LegalSecurityEnforcementStage.MODEL_VISIBLE_EVIDENCE));
    }

    @Test
    void shouldProtectResourcesBeforeModelExposure() {

        assertTrue(
                pipeline.occursBefore(
                        LegalSecurityEnforcementStage.RESOURCE_PROTECTION,
                        LegalSecurityEnforcementStage.MODEL_VISIBLE_EVIDENCE));
    }

    @Test
    void shouldApplyEvidenceTrustBeforeResourceProtection() {

        assertTrue(
                pipeline.occursBefore(
                        LegalSecurityEnforcementStage.EVIDENCE_TRUST,
                        LegalSecurityEnforcementStage.RESOURCE_PROTECTION));
    }

    @Test
    void shouldApplySkillBoundaryBeforeToolAuthorization() {

        assertTrue(
                pipeline.occursBefore(
                        LegalSecurityEnforcementStage.SKILL_BOUNDARY,
                        LegalSecurityEnforcementStage.TOOL_AUTHORIZATION));
    }

    @Test
    void shouldNotExecuteToolBeforeAuthorization() {

        assertFalse(
                pipeline.occursBefore(
                        LegalSecurityEnforcementStage.TOOL_EXECUTION,
                        LegalSecurityEnforcementStage.TOOL_AUTHORIZATION));
    }

    @Test
    void shouldRejectNullStage() {

        assertThrows(
                NullPointerException.class,
                () -> pipeline.positionOf(
                        null));
    }
}
