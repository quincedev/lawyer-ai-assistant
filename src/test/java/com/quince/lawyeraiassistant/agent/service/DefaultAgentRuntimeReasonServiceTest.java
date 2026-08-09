package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.prompt.builder.RuntimeReasonPromptContextBuilder;
import com.quince.lawyeraiassistant.agent.prompt.model.RuntimeReasonPromptContext;
import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAgentRuntimeReasonServiceTest {

    private ChatClient chatClient;

    private PromptBuilder promptBuilder;

    private RuntimeReasonPromptContextBuilder promptContextBuilder;

    private ChatClientRequestSpec requestSpec;

    private CallResponseSpec responseSpec;

    private DefaultAgentRuntimeReasonService service;

    @BeforeEach
    void setUp() {

        chatClient = mock(
                ChatClient.class);

        promptBuilder = mock(
                PromptBuilder.class);

        promptContextBuilder = mock(
                RuntimeReasonPromptContextBuilder.class);

        requestSpec = mock(
                ChatClientRequestSpec.class);

        responseSpec = mock(
                CallResponseSpec.class);

        service = new DefaultAgentRuntimeReasonService(
                chatClient,
                promptBuilder,
                promptContextBuilder);
    }

    @Test
    void shouldGenerateRuntimeReason() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        AgentTask task = AgentTask.pending(
                "task-2",
                "分析竞业限制条款");

        RuntimeReasonPromptContext promptContext = new RuntimeReasonPromptContext(
                "分析劳动合同",
                "task-2 | RUNNING | 分析竞业限制条款",
                "当前执行计划",
                "合同约定竞业限制24个月");

        Prompt prompt = mock(
                Prompt.class);

        when(
                promptContextBuilder.build(
                        context,
                        task))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildRuntimeReason(
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
                        "  该竞业限制条款需要进一步结合经济补偿约定判断。  ");

        String result = service.reason(
                context,
                task);

        assertEquals(
                "该竞业限制条款需要进一步结合经济补偿约定判断。",
                result);

        verify(
                promptContextBuilder)
                .build(
                        context,
                        task);

        verify(
                promptBuilder)
                .buildRuntimeReason(
                        promptContext);
    }

    @Test
    void shouldRejectNullContext() {

        AgentTask task = AgentTask.pending(
                "task-1",
                "分析合同");

        assertThrows(
                NullPointerException.class,
                () -> service.reason(
                        null,
                        task));
    }

    @Test
    void shouldRejectNullTask() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        assertThrows(
                NullPointerException.class,
                () -> service.reason(
                        context,
                        null));
    }

    @Test
    void shouldRejectBlankResult() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        AgentTask task = AgentTask.pending(
                "task-1",
                "分析合同");

        RuntimeReasonPromptContext promptContext = new RuntimeReasonPromptContext(
                "分析劳动合同",
                "task-1 | RUNNING | 分析合同",
                "当前计划",
                "已有结果");

        Prompt prompt = mock(
                Prompt.class);

        when(
                promptContextBuilder.build(
                        context,
                        task))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildRuntimeReason(
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
                () -> service.reason(
                        context,
                        task));

        assertEquals(
                "Runtime reason result must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullResult() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        AgentTask task = AgentTask.pending(
                "task-1",
                "分析合同");

        RuntimeReasonPromptContext promptContext = new RuntimeReasonPromptContext(
                "分析劳动合同",
                "task-1 | RUNNING | 分析合同",
                "当前计划",
                "已有结果");

        Prompt prompt = mock(
                Prompt.class);

        when(
                promptContextBuilder.build(
                        context,
                        task))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildRuntimeReason(
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

        assertThrows(
                IllegalStateException.class,
                () -> service.reason(
                        context,
                        task));
    }
}