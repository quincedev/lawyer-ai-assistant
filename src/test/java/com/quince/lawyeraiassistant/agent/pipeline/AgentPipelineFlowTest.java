package com.quince.lawyeraiassistant.agent.pipeline;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ReasonResult;
import com.quince.lawyeraiassistant.agent.operator.AgentOperator;
import com.quince.lawyeraiassistant.agent.operator.SpringAiPlanningOperator;
import com.quince.lawyeraiassistant.agent.operator.SpringAiReasonOperator;
import com.quince.lawyeraiassistant.agent.prompt.model.PlanningPromptContext;
import com.quince.lawyeraiassistant.agent.prompt.model.ReasonPromptContext;
import com.quince.lawyeraiassistant.agent.service.AgentPlanningService;
import com.quince.lawyeraiassistant.agent.service.AgentReasonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentPipelineFlowTest {

    private AgentReasonService reasonService;
    private AgentPlanningService planningService;
    private AgentPipeline pipeline;

    @BeforeEach
    void setUp() {
        reasonService = mock(AgentReasonService.class);
        planningService = mock(AgentPlanningService.class);

        List<AgentOperator> initializationOperators = List.of(
                new SpringAiPlanningOperator(planningService),
                new SpringAiReasonOperator(reasonService));

        pipeline = new DefaultAgentPipeline(initializationOperators);
    }

    @Test
    void shouldExecuteReasonThenPlanningInitializationChain() {
        ReasonResult reasonResult = ReasonResult.from("用户希望识别劳动合同法律风险");
        AgentPlan plan = AgentPlan.from(List.of(
                AgentTask.pending("task-1", "读取劳动合同"),
                AgentTask.pending("task-2", "识别法律风险")));

        when(reasonService.reason(any(ReasonPromptContext.class))).thenReturn(reasonResult);
        when(planningService.plan(any(PlanningPromptContext.class))).thenReturn(plan);

        AgentContext result = pipeline.execute(AgentContext.from("分析劳动合同"));

        assertSame(reasonResult, result.getReasonResult());
        assertSame(plan, result.getAgentPlan());
        assertEquals(AgentStatus.RUNNING, result.getStatus());
        assertEquals(List.of("Reason completed", "Planning completed"), result.getExecutionLogs());

        var ordered = inOrder(reasonService, planningService);
        ordered.verify(reasonService).reason(any(ReasonPromptContext.class));
        ordered.verify(planningService).plan(any(PlanningPromptContext.class));
    }

    @Test
    void shouldPassReasonResultIntoPlanningContext() {
        ReasonResult reasonResult = ReasonResult.from("需要分析竞业限制条款");
        AgentPlan plan = AgentPlan.from(List.of(
                AgentTask.pending("task-1", "分析竞业限制条款")));

        when(reasonService.reason(any(ReasonPromptContext.class))).thenReturn(reasonResult);
        when(planningService.plan(any(PlanningPromptContext.class))).thenReturn(plan);

        pipeline.execute(AgentContext.from("分析劳动合同"));

        verify(planningService).plan(
                PlanningPromptContext.from("分析劳动合同", reasonResult));
    }
}
