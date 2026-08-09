package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ReflectionDecision;
import com.quince.lawyeraiassistant.agent.model.ReflectionResult;
import com.quince.lawyeraiassistant.agent.parser.AgentPlanParser;
import com.quince.lawyeraiassistant.agent.prompt.builder.ReplanningPromptContextBuilder;
import com.quince.lawyeraiassistant.agent.prompt.model.ReplanningPromptContext;
import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAgentReplanningServiceTest {

    private ChatClient chatClient;

    private PromptBuilder promptBuilder;

    private ReplanningPromptContextBuilder promptContextBuilder;

    private AgentPlanParser agentPlanParser;

    private ChatClientRequestSpec requestSpec;

    private CallResponseSpec responseSpec;

    private DefaultAgentReplanningService replanningService;

    @BeforeEach
    void setUp() {

        chatClient = mock(
                ChatClient.class);

        promptBuilder = mock(
                PromptBuilder.class);

        promptContextBuilder = mock(
                ReplanningPromptContextBuilder.class);

        agentPlanParser = mock(
                AgentPlanParser.class);

        requestSpec = mock(
                ChatClientRequestSpec.class);

        responseSpec = mock(
                CallResponseSpec.class);

        replanningService = new DefaultAgentReplanningService(
                chatClient,
                promptBuilder,
                promptContextBuilder,
                agentPlanParser);
    }

    @Test
    void shouldGenerateReplannedAgentPlan() {

        AgentContext context = AgentContext.from(
                "分析劳动合同并生成律师意见书");

        ReflectionResult reflectionResult = ReflectionResult.of(
                ReflectionDecision.REPLAN,
                "原计划没有覆盖竞业限制风险");

        ReplanningPromptContext promptContext = new ReplanningPromptContext(
                "分析劳动合同并生成律师意见书",
                "用户希望分析劳动合同风险",
                "task-1 | COMPLETED | 读取劳动合同",
                "已经识别竞业限制条款",
                "原计划没有覆盖竞业限制风险");

        Prompt prompt = mock(
                Prompt.class);

        String content = """
                task-2|分析竞业限制条款
                task-3|检索相关法律依据
                task-4|生成律师意见书
                """;

        AgentPlan expectedPlan = AgentPlan.from(
                List.of(
                        AgentTask.pending(
                                "task-2",
                                "分析竞业限制条款"),
                        AgentTask.pending(
                                "task-3",
                                "检索相关法律依据"),
                        AgentTask.pending(
                                "task-4",
                                "生成律师意见书")));

        when(
                promptContextBuilder.build(
                        context,
                        reflectionResult))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildReplanning(
                        promptContext))
                .thenReturn(
                        prompt);

        when(
                chatClient.prompt(
                        prompt))
                .thenReturn(
                        requestSpec);

        when(
                requestSpec.call())
                .thenReturn(
                        responseSpec);

        when(
                responseSpec.content())
                .thenReturn(
                        content);

        when(
                agentPlanParser.parse(
                        content))
                .thenReturn(
                        expectedPlan);

        AgentPlan result = replanningService.replan(
                context,
                reflectionResult);

        assertSame(
                expectedPlan,
                result);

        verify(
                promptContextBuilder)
                .build(
                        context,
                        reflectionResult);

        verify(
                promptBuilder)
                .buildReplanning(
                        promptContext);

        verify(
                agentPlanParser)
                .parse(
                        content);
    }

    @Test
    void shouldRejectNullContext() {

        ReflectionResult reflectionResult = ReflectionResult.of(
                ReflectionDecision.REPLAN,
                "需要重新规划");

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> replanningService.replan(
                        null,
                        reflectionResult));

        assertEquals(
                "AgentContext must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullReflectionResult() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> replanningService.replan(
                        context,
                        null));

        assertEquals(
                "ReflectionResult must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectContinueDecision() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        ReflectionResult reflectionResult = ReflectionResult.of(
                ReflectionDecision.CONTINUE,
                "当前任务已经完成");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> replanningService.replan(
                        context,
                        reflectionResult));

        assertEquals(
                "Replanning requires REPLAN reflection decision",
                exception.getMessage());
    }

    @Test
    void shouldRejectRetryDecision() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        ReflectionResult reflectionResult = ReflectionResult.of(
                ReflectionDecision.RETRY,
                "当前结果不足");

        assertThrows(
                IllegalArgumentException.class,
                () -> replanningService.replan(
                        context,
                        reflectionResult));
    }

    @Test
    void shouldRejectFinishDecision() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        ReflectionResult reflectionResult = ReflectionResult.of(
                ReflectionDecision.FINISH,
                "已有结果足以完成目标");

        assertThrows(
                IllegalArgumentException.class,
                () -> replanningService.replan(
                        context,
                        reflectionResult));
    }

    @Test
    void shouldRejectBlankReplanningResult() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        ReflectionResult reflectionResult = ReflectionResult.of(
                ReflectionDecision.REPLAN,
                "需要调整计划");

        ReplanningPromptContext promptContext = new ReplanningPromptContext(
                "分析劳动合同",
                null,
                null,
                null,
                "需要调整计划");

        Prompt prompt = mock(
                Prompt.class);

        when(
                promptContextBuilder.build(
                        context,
                        reflectionResult))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildReplanning(
                        promptContext))
                .thenReturn(
                        prompt);

        when(
                chatClient.prompt(
                        prompt))
                .thenReturn(
                        requestSpec);

        when(
                requestSpec.call())
                .thenReturn(
                        responseSpec);

        when(
                responseSpec.content())
                .thenReturn(
                        "   ");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> replanningService.replan(
                        context,
                        reflectionResult));

        assertEquals(
                "Replanning result must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullReplanningResult() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        ReflectionResult reflectionResult = ReflectionResult.of(
                ReflectionDecision.REPLAN,
                "需要调整计划");

        ReplanningPromptContext promptContext = new ReplanningPromptContext(
                "分析劳动合同",
                null,
                null,
                null,
                "需要调整计划");

        Prompt prompt = mock(
                Prompt.class);

        when(
                promptContextBuilder.build(
                        context,
                        reflectionResult))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildReplanning(
                        promptContext))
                .thenReturn(
                        prompt);

        when(
                chatClient.prompt(
                        prompt))
                .thenReturn(
                        requestSpec);

        when(
                requestSpec.call())
                .thenReturn(
                        responseSpec);

        when(
                responseSpec.content())
                .thenReturn(
                        null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> replanningService.replan(
                        context,
                        reflectionResult));

        assertEquals(
                "Replanning result must not be blank",
                exception.getMessage());
    }
}