package com.quince.lawyeraiassistant.agent.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AgentActionDecisionTest {

    @Test
    void shouldCreateToolDecision() {

        AgentActionDecision decision = new AgentActionDecision(
                AgentActionType.TOOL,
                "searchLegalKnowledge",
                Map.of(
                        "legalQuestion",
                        "违法解除劳动合同"));

        assertEquals(
                AgentActionType.TOOL,
                decision.actionType());

        assertEquals(
                "searchLegalKnowledge",
                decision.toolName());

        assertEquals(
                "违法解除劳动合同",
                decision.arguments()
                        .get("legalQuestion"));
    }

    @Test
    void shouldCreateReasonDecision() {

        AgentActionDecision decision = new AgentActionDecision(
                AgentActionType.REASON,
                null,
                null);

        assertEquals(
                AgentActionType.REASON,
                decision.actionType());

        assertNull(
                decision.toolName());

        assertNull(
                decision.arguments());
    }

    @Test
    void shouldCreateFinalAnswerDecision() {

        AgentActionDecision decision = new AgentActionDecision(
                AgentActionType.FINAL_ANSWER,
                null,
                null);

        assertEquals(
                AgentActionType.FINAL_ANSWER,
                decision.actionType());

        assertNull(
                decision.toolName());

        assertNull(
                decision.arguments());
    }

    @Test
    void shouldSupportLegacyToolDecisionConstructor() {

        AgentActionDecision decision = new AgentActionDecision(
                "searchLegalKnowledge",
                Map.of(
                        "legalQuestion",
                        "劳动合同法第八十七条"));

        assertEquals(
                AgentActionType.TOOL,
                decision.actionType());

        assertEquals(
                "searchLegalKnowledge",
                decision.toolName());

        assertEquals(
                "劳动合同法第八十七条",
                decision.arguments()
                        .get("legalQuestion"));
    }

    @Test
    void shouldAllowRawExternalDecisionForLaterValidation() {

        AgentActionDecision decision = new AgentActionDecision(
                AgentActionType.REASON,
                "searchLegalKnowledge",
                Map.of(
                        "unexpected",
                        "value"));

        assertEquals(
                AgentActionType.REASON,
                decision.actionType());

        assertEquals(
                "searchLegalKnowledge",
                decision.toolName());

        assertEquals(
                "value",
                decision.arguments()
                        .get("unexpected"));
    }
}