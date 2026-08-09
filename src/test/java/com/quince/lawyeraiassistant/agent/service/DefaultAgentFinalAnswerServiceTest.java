package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.prompt.builder.FinalAnswerPromptContextBuilder;
import com.quince.lawyeraiassistant.agent.prompt.model.FinalAnswerPromptContext;
import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAgentFinalAnswerServiceTest {

    private ChatClient chatClient;

    private ChatClient.ChatClientRequestSpec requestSpec;

    private ChatClient.CallResponseSpec callResponseSpec;

    private PromptBuilder promptBuilder;

    private FinalAnswerPromptContextBuilder promptContextBuilder;

    private DefaultAgentFinalAnswerService service;

    private Prompt prompt;

    @BeforeEach
    void setUp() {

        chatClient = mock(
                ChatClient.class);

        requestSpec = mock(
                ChatClient.ChatClientRequestSpec.class);

        callResponseSpec = mock(
                ChatClient.CallResponseSpec.class);

        promptBuilder = mock(
                PromptBuilder.class);

        promptContextBuilder = mock(
                FinalAnswerPromptContextBuilder.class);

        prompt = mock(
                Prompt.class);

        service = new DefaultAgentFinalAnswerService(
                chatClient,
                promptBuilder,
                promptContextBuilder);
    }

    @Test
    void shouldGenerateFinalAnswer() {

        AgentContext context = AgentContext.from(
                "分析劳动合同并生成律师意见");

        FinalAnswerPromptContext promptContext = new FinalAnswerPromptContext(
                "分析劳动合同并生成律师意见",
                "用户希望分析劳动合同风险",
                "task-1 | COMPLETED | 查询法律依据",
                "劳动合同法相关检索结果");

        when(
                promptContextBuilder.build(
                        context))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildFinalAnswer(
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
                        callResponseSpec);

        when(
                callResponseSpec.content())
                .thenReturn(
                        "  根据现有材料，该劳动合同存在以下法律风险。  ");

        String result = service.generate(
                context);

        assertEquals(
                "根据现有材料，该劳动合同存在以下法律风险。",
                result);

        verify(
                promptContextBuilder)
                .build(
                        context);

        verify(
                promptBuilder)
                .buildFinalAnswer(
                        promptContext);

        verify(
                chatClient)
                .prompt(
                        prompt);

        verify(
                requestSpec)
                .call();

        verify(
                callResponseSpec)
                .content();
    }

    @Test
    void shouldRejectNullContext() {

        assertThrows(
                NullPointerException.class,
                () -> service.generate(
                        null));
    }

    @Test
    void shouldRejectNullFinalAnswer() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        FinalAnswerPromptContext promptContext = new FinalAnswerPromptContext(
                "分析劳动合同",
                null,
                null,
                null);

        when(
                promptContextBuilder.build(
                        context))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildFinalAnswer(
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
                        callResponseSpec);

        when(
                callResponseSpec.content())
                .thenReturn(
                        null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.generate(
                        context));

        assertEquals(
                "Final answer must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankFinalAnswer() {

        AgentContext context = AgentContext.from(
                "分析劳动合同");

        FinalAnswerPromptContext promptContext = new FinalAnswerPromptContext(
                "分析劳动合同",
                null,
                null,
                null);

        when(
                promptContextBuilder.build(
                        context))
                .thenReturn(
                        promptContext);

        when(
                promptBuilder.buildFinalAnswer(
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
                        callResponseSpec);

        when(
                callResponseSpec.content())
                .thenReturn(
                        "     ");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.generate(
                        context));

        assertEquals(
                "Final answer must not be blank",
                exception.getMessage());
    }
}