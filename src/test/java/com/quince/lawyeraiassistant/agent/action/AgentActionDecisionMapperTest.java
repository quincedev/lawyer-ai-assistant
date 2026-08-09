package com.quince.lawyeraiassistant.agent.action;

import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionDecision;
import com.quince.lawyeraiassistant.agent.model.AgentActionType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentActionDecisionMapperTest {

        private final AgentActionDecisionMapper mapper = new AgentActionDecisionMapper();

        @Test
        void shouldMapToolDecision() {

                AgentActionDecision decision = new AgentActionDecision(
                                AgentActionType.TOOL,
                                "searchLegalKnowledge",
                                Map.of(
                                                "legalQuestion",
                                                "违法解除劳动合同"));

                AgentAction action = mapper.map(
                                "task-1",
                                decision);

                assertTrue(
                                action.isTool());

                assertEquals(
                                AgentActionType.TOOL,
                                action.getType());

                assertEquals(
                                "task-1",
                                action.getTaskId());

                assertEquals(
                                "searchLegalKnowledge",
                                action.requireToolAction()
                                                .getToolName());

                assertEquals(
                                "违法解除劳动合同",
                                action.requireToolAction()
                                                .getArguments()
                                                .get("legalQuestion"));
        }

        @Test
        void shouldMapReasonDecision() {

                AgentActionDecision decision = new AgentActionDecision(
                                AgentActionType.REASON,
                                null,
                                null);

                AgentAction action = mapper.map(
                                "task-2",
                                decision);

                assertTrue(
                                action.isReason());

                assertEquals(
                                "task-2",
                                action.getTaskId());

                assertNull(
                                action.getToolAction());
        }

        @Test
        void shouldMapFinalAnswerDecision() {

                AgentActionDecision decision = new AgentActionDecision(
                                AgentActionType.FINAL_ANSWER,
                                null,
                                null);

                AgentAction action = mapper.map(
                                "task-3",
                                decision);

                assertTrue(
                                action.isFinalAnswer());

                assertEquals(
                                "task-3",
                                action.getTaskId());

                assertNull(
                                action.getToolAction());
        }

        @Test
        void shouldRejectToolDecisionWithoutToolName() {

                AgentActionDecision decision = new AgentActionDecision(
                                AgentActionType.TOOL,
                                null,
                                Map.of());

                assertThrows(
                                NullPointerException.class,
                                () -> mapper.map(
                                                "task-1",
                                                decision));
        }
}