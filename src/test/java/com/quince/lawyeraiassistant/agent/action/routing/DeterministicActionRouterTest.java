package com.quince.lawyeraiassistant.agent.action.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionType;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.tool.legal.LegalToolContract;

class DeterministicActionRouterTest {

    private static final String TASK_ID = "task-1";

    private final DeterministicActionRouter router = new DeterministicActionRouter();

    @Test
    void shouldRouteExplicitRetrievalTaskToTool() {
        AgentAction action = route(
                AgentContext.from("处理案件"),
                "检索与本案相关的法律依据");

        assertEquals(AgentActionType.TOOL, action.getType());
        assertEquals(TASK_ID, action.getTaskId());
        assertEquals("searchLegalKnowledge", action.requireToolAction().getToolName());
        assertEquals(
                "检索与本案相关的法律依据",
                action.requireToolAction()
                        .getArguments()
                        .get(
                                LegalToolContract.LEGAL_QUESTION));
    }

    @Test
    void shouldRouteCoreDisputeAndFrameworkIdentificationToReason() {
        AgentAction action = route(
                AgentContext.from("处理案件"),
                "识别核心争议点和框架");

        assertEquals(AgentActionType.REASON, action.getType());
        assertEquals(TASK_ID, action.getTaskId());
    }

    @Test
    void shouldRouteAnalyticalTaskToReasonWhenSuccessfulEvidenceExists() {
        AgentContext context = AgentContext.from("处理案件")
                .appendObservation(ToolObservation.success(
                        "evidence-task",
                        "searchLegalKnowledge",
                        "相关法律依据"));

        AgentAction action = route(context, "分析案件的法律风险");

        assertEquals(AgentActionType.REASON, action.getType());
        assertEquals(TASK_ID, action.getTaskId());
    }

    @Test
    void shouldRouteTaskToReasonWhenSameTaskHasSuccessfulEvidence() {
        AgentContext context = AgentContext.from("处理案件")
                .appendObservation(ToolObservation.success(
                        TASK_ID,
                        "searchLegalKnowledge",
                        "当前任务已有证据"));

        AgentAction action = route(context, "继续处理当前事项");

        assertEquals(AgentActionType.REASON, action.getType());
        assertEquals(TASK_ID, action.getTaskId());
    }

    @Test
    void shouldReturnEmptyForAmbiguousTask() {
        Optional<AgentAction> action = router.route(
                AgentContext.from("处理案件"),
                AgentTask.pending(TASK_ID, "继续处理当前事项"));

        assertTrue(action.isEmpty());
    }

    @Test
    void shouldRouteLegalScopeIdentificationTaskToReason() {

        AgentAction action = route(
                AgentContext.from(
                        "处理案件"),
                """
                        识别劳动合同解除问题的核心法律争议点，
                        确定需检索的法律依据范围与检索方向
                        """);

        assertEquals(
                AgentActionType.REASON,
                action.getType());
    }

    private AgentAction route(
            AgentContext context,
            String description) {

        return router.route(
                context,
                AgentTask.pending(TASK_ID, description))
                .orElseThrow();
    }
}