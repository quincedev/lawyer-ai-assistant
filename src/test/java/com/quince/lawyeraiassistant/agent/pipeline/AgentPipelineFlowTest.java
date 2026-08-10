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
import com.quince.lawyeraiassistant.agent.skill.AgentSkill;
import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;
import com.quince.lawyeraiassistant.agent.skill.scope.SkillToolScope;
import com.quince.lawyeraiassistant.agent.tool.AgentToolRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentPipelineFlowTest {

    private AgentReasonService reasonService;

    private AgentPlanningService planningService;

    private SkillToolScope skillToolScope;

    private AgentToolRegistry toolRegistry;

    private AgentPipeline pipeline;

    @BeforeEach
    void setUp() {

        reasonService = mock(
                AgentReasonService.class);

        planningService = mock(
                AgentPlanningService.class);

        skillToolScope = mock(
                SkillToolScope.class);

        toolRegistry = mock(
                AgentToolRegistry.class);

        /*
         * 默认场景：
         * Tool Registry 中没有当前 Agent 可使用的 Tool。
         *
         * 这样 Generic Agent 默认得到：
         * availableTools = "无"
         */
        when(
                toolRegistry.names())
                .thenReturn(
                        List.of());

        when(
                skillToolScope.filterAllowed(
                        any(),
                        any()))
                .thenReturn(
                        List.of());

        List<AgentOperator> initializationOperators = List.of(
                new SpringAiPlanningOperator(
                        planningService,
                        skillToolScope,
                        toolRegistry),
                new SpringAiReasonOperator(
                        reasonService));

        pipeline = new DefaultAgentPipeline(
                initializationOperators);
    }

    @Test
    void shouldExecuteReasonThenPlanningInitializationChain() {

        ReasonResult reasonResult = ReasonResult.from(
                "用户希望识别劳动合同法律风险");

        AgentPlan plan = AgentPlan.from(
                List.of(
                        AgentTask.pending(
                                "task-1",
                                "读取劳动合同"),
                        AgentTask.pending(
                                "task-2",
                                "识别法律风险")));

        when(
                reasonService.reason(
                        any(
                                ReasonPromptContext.class)))
                .thenReturn(
                        reasonResult);

        when(
                planningService.plan(
                        any(
                                PlanningPromptContext.class)))
                .thenReturn(
                        plan);

        AgentContext result = pipeline.execute(
                AgentContext.from(
                        "分析劳动合同"));

        assertSame(
                reasonResult,
                result.getReasonResult());

        assertSame(
                plan,
                result.getAgentPlan());

        assertEquals(
                AgentStatus.RUNNING,
                result.getStatus());

        assertEquals(
                List.of(
                        "Reason completed",
                        "Planning completed"),
                result.getExecutionLogs());

        var ordered = inOrder(
                reasonService,
                planningService);

        ordered.verify(
                reasonService)
                .reason(
                        any(
                                ReasonPromptContext.class));

        ordered.verify(
                planningService)
                .plan(
                        any(
                                PlanningPromptContext.class));
    }

    @Test
    void shouldPassReasonResultAndNoToolsIntoPlanningContext() {

        ReasonResult reasonResult = ReasonResult.from(
                "需要分析人工智能 Agent 与 Workflow 的区别");

        AgentPlan plan = AgentPlan.from(
                List.of(
                        AgentTask.pending(
                                "task-1",
                                "梳理核心概念")));

        when(
                reasonService.reason(
                        any(
                                ReasonPromptContext.class)))
                .thenReturn(
                        reasonResult);

        when(
                planningService.plan(
                        any(
                                PlanningPromptContext.class)))
                .thenReturn(
                        plan);

        pipeline.execute(
                AgentContext.from(
                        "分析人工智能 Agent 和传统 Workflow 的主要区别"));

        ArgumentCaptor<PlanningPromptContext> captor = ArgumentCaptor.forClass(
                PlanningPromptContext.class);

        verify(
                planningService)
                .plan(
                        captor.capture());

        PlanningPromptContext planningContext = captor.getValue();

        assertEquals(
                "分析人工智能 Agent 和传统 Workflow 的主要区别",
                planningContext.getGoal());

        assertSame(
                reasonResult,
                planningContext.getReasonResult());

        assertEquals(
                "无",
                planningContext.getSkillInstructions());

        assertEquals(
                "无",
                planningContext.getAvailableTools());

        verify(
                skillToolScope)
                .filterAllowed(
                        any(),
                        any());
    }

    @Test
    void shouldPassSkillInstructionsAndAvailableToolsIntoPlanningContext() {

        ReasonResult reasonResult = ReasonResult.from(
                "需要检索违法解除劳动合同相关法律依据");

        AgentPlan plan = AgentPlan.from(
                List.of(
                        AgentTask.pending(
                                "task-1",
                                "检索违法解除劳动合同法律依据"),
                        AgentTask.pending(
                                "task-2",
                                "分析违法解除法律责任")));

        AgentSkill legalResearchSkill = AgentSkill.of(
                "legal-research",
                "Legal Research",
                "用于法律问题研究与法律依据检索",
                "优先检索法律知识库，并基于检索结果进行法律分析",
                List.of(
                        "searchLegalKnowledge"),
                Set.of(
                        "legal",
                        "research"));

        SkillContext skillContext = SkillContext.of(
                legalResearchSkill);

        when(
                toolRegistry.names())
                .thenReturn(
                        List.of(
                                "searchLegalKnowledge"));

        when(
                skillToolScope.filterAllowed(
                        any(),
                        any()))
                .thenReturn(
                        List.of(
                                "searchLegalKnowledge"));

        when(
                reasonService.reason(
                        any(
                                ReasonPromptContext.class)))
                .thenReturn(
                        reasonResult);

        when(
                planningService.plan(
                        any(
                                PlanningPromptContext.class)))
                .thenReturn(
                        plan);

        AgentContext initialContext = AgentContext.from(
                "分析违法解除劳动合同")
                .withSkillContext(
                        skillContext);

        pipeline.execute(
                initialContext);

        ArgumentCaptor<PlanningPromptContext> captor = ArgumentCaptor.forClass(
                PlanningPromptContext.class);

        verify(
                planningService)
                .plan(
                        captor.capture());

        PlanningPromptContext planningContext = captor.getValue();

        assertEquals(
                "分析违法解除劳动合同",
                planningContext.getGoal());

        assertSame(
                reasonResult,
                planningContext.getReasonResult());

        assertEquals(
                "优先检索法律知识库，并基于检索结果进行法律分析",
                planningContext.getSkillInstructions());

        assertEquals(
                "searchLegalKnowledge",
                planningContext.getAvailableTools());
    }

    @Test
    void shouldPreserveSkillContextThroughReasonAndPlanning() {

        ReasonResult reasonResult = ReasonResult.from(
                "需要研究劳动合同解除法律依据");

        AgentPlan plan = AgentPlan.from(
                List.of(
                        AgentTask.pending(
                                "task-1",
                                "检索劳动合同解除法律依据")));

        AgentSkill legalResearchSkill = AgentSkill.of(
                "legal-research",
                "Legal Research",
                "用于法律问题研究与法律依据检索",
                "优先检索法律知识库",
                List.of(
                        "searchLegalKnowledge"),
                Set.of(
                        "legal",
                        "research"));

        SkillContext skillContext = SkillContext.of(
                legalResearchSkill);

        when(
                toolRegistry.names())
                .thenReturn(
                        List.of(
                                "searchLegalKnowledge"));

        when(
                skillToolScope.filterAllowed(
                        any(),
                        any()))
                .thenReturn(
                        List.of(
                                "searchLegalKnowledge"));

        when(
                reasonService.reason(
                        any(
                                ReasonPromptContext.class)))
                .thenReturn(
                        reasonResult);

        when(
                planningService.plan(
                        any(
                                PlanningPromptContext.class)))
                .thenReturn(
                        plan);

        AgentContext initialContext = AgentContext.from(
                "分析劳动合同解除问题")
                .withSkillContext(
                        skillContext);

        AgentContext result = pipeline.execute(
                initialContext);

        assertTrue(
                result.hasSkill());

        assertSame(
                skillContext,
                result.getSkillContext()
                        .orElseThrow());

        assertSame(
                legalResearchSkill,
                result.getSelectedSkill()
                        .orElseThrow());

        assertEquals(
                "legal-research",
                result.getSkillContext()
                        .orElseThrow()
                        .getSkillId());

        assertEquals(
                "优先检索法律知识库",
                result.getSkillContext()
                        .orElseThrow()
                        .getInstructions());

        assertSame(
                reasonResult,
                result.getReasonResult());

        assertSame(
                plan,
                result.getAgentPlan());
    }
}