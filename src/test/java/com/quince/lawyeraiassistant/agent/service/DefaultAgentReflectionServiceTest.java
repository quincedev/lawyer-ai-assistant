package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ReflectionDecision;
import com.quince.lawyeraiassistant.agent.model.ReflectionDecisionResponse;
import com.quince.lawyeraiassistant.agent.model.ReflectionResult;
import com.quince.lawyeraiassistant.agent.prompt.builder.ReflectionPromptContextBuilder;
import com.quince.lawyeraiassistant.agent.prompt.model.ReflectionPromptContext;
import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultAgentReflectionServiceTest {

    private ChatClient chatClient;

    private PromptBuilder promptBuilder;

    private ReflectionPromptContextBuilder promptContextBuilder;

    private ChatClientRequestSpec requestSpec;

    private CallResponseSpec responseSpec;

    private DefaultAgentReflectionService reflectionService;

    @BeforeEach
    void setUp() {

        chatClient = mock(
                ChatClient.class);

        promptBuilder = mock(
                PromptBuilder.class);

        promptContextBuilder = mock(
                ReflectionPromptContextBuilder.class);

        requestSpec = mock(
                ChatClientRequestSpec.class);

        responseSpec = mock(
                CallResponseSpec.class);

        reflectionService = new DefaultAgentReflectionService(
                chatClient,
                promptBuilder,
                promptContextBuilder);
    }

    @Test
    void shouldReflectAndReturnContinueDecision() {

        AgentContext context = AgentContext.from(
                "分析违法解除劳动合同");

        AgentTask task = AgentTask.pending(
                "task-1",
                "查询违法解除法律责任");

        ReflectionPromptContext promptContext = new ReflectionPromptContext(
                "分析违法解除劳动合同",
                "用户希望分析违法解除的法律责任",
                "task-1",
                "查询违法解除法律责任",
                "task-1 | RUNNING",
                "已经检索到违法解除赔偿规定");

        Prompt prompt = mock(
                Prompt.class);

        ReflectionDecisionResponse response = new ReflectionDecisionResponse(
                ReflectionDecision.CONTINUE,
                "当前结果已经足以完成当前任务");

        when(
                promptContextBuilder.build(
                        context,
                        task))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildReflection(
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
                responseSpec.entity(
                        ReflectionDecisionResponse.class))
                .thenReturn(
                        response);

        ReflectionResult result = reflectionService.reflect(
                context,
                task);

        assertEquals(
                ReflectionDecision.CONTINUE,
                result.getDecision());

        assertEquals(
                "当前结果已经足以完成当前任务",
                result.getSummary());

        assertTrue(
                result.shouldContinue());
    }

    @Test
    void shouldReturnRetryDecision() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        AgentTask task = AgentTask.pending(
                "task-1",
                "查询法律依据");

        ReflectionPromptContext promptContext = new ReflectionPromptContext(
                "分析劳动合同",
                null,
                "task-1",
                "查询法律依据",
                null,
                null);

        Prompt prompt = mock(
                Prompt.class);

        when(
                promptContextBuilder.build(
                        context,
                        task))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildReflection(
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
                responseSpec.entity(
                        ReflectionDecisionResponse.class))
                .thenReturn(
                        new ReflectionDecisionResponse(
                                ReflectionDecision.RETRY,
                                "当前结果不足"));

        ReflectionResult result = reflectionService.reflect(
                context,
                task);

        assertTrue(
                result.shouldRetry());
    }

    @Test
    void shouldRejectNullContext() {

        AgentTask task = AgentTask.pending(
                "task-1",
                "分析合同风险");

        assertThrows(
                NullPointerException.class,
                () -> reflectionService.reflect(
                        null,
                        task));
    }

    @Test
    void shouldRejectNullTask() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        assertThrows(
                NullPointerException.class,
                () -> reflectionService.reflect(
                        context,
                        null));
    }

    @Test
    void shouldRejectNullReflectionResponse() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        AgentTask task = AgentTask.pending(
                "task-1",
                "查询法律依据");

        ReflectionPromptContext promptContext = new ReflectionPromptContext(
                "分析劳动合同",
                null,
                "task-1",
                "查询法律依据",
                null,
                null);

        Prompt prompt = mock(
                Prompt.class);

        when(
                promptContextBuilder.build(
                        context,
                        task))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildReflection(
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
                responseSpec.entity(
                        ReflectionDecisionResponse.class))
                .thenReturn(
                        null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reflectionService.reflect(
                        context,
                        task));

        assertEquals(
                "Reflection result must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullReflectionDecision() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        AgentTask task = AgentTask.pending(
                "task-1",
                "查询法律依据");

        ReflectionPromptContext promptContext = new ReflectionPromptContext(
                "分析劳动合同",
                null,
                "task-1",
                "查询法律依据",
                null,
                null);

        Prompt prompt = mock(
                Prompt.class);

        when(
                promptContextBuilder.build(
                        context,
                        task))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildReflection(
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
                responseSpec.entity(
                        ReflectionDecisionResponse.class))
                .thenReturn(
                        new ReflectionDecisionResponse(
                                null,
                                "当前结果足够"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reflectionService.reflect(
                        context,
                        task));

        assertEquals(
                "Reflection decision must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankReflectionSummary() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        AgentTask task = AgentTask.pending(
                "task-1",
                "查询法律依据");

        ReflectionPromptContext promptContext = new ReflectionPromptContext(
                "分析劳动合同",
                null,
                "task-1",
                "查询法律依据",
                null,
                null);

        Prompt prompt = mock(
                Prompt.class);

        when(
                promptContextBuilder.build(
                        context,
                        task))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildReflection(
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
                responseSpec.entity(
                        ReflectionDecisionResponse.class))
                .thenReturn(
                        new ReflectionDecisionResponse(
                                ReflectionDecision.CONTINUE,
                                "   "));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reflectionService.reflect(
                        context,
                        task));

        assertEquals(
                "Reflection summary must not be blank",
                exception.getMessage());
    }
}