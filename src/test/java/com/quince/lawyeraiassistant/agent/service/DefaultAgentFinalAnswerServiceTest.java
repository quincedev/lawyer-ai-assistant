package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.prompt.builder.FinalAnswerPromptContextBuilder;
import com.quince.lawyeraiassistant.agent.prompt.model.FinalAnswerPromptContext;
import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAgentFinalAnswerServiceTest {

        private ChatClient chatClient;
        private ChatClient.ChatClientRequestSpec requestSpec;
        private ChatClient.CallResponseSpec callResponseSpec;
        private ChatClient.StreamResponseSpec streamResponseSpec;
        private PromptBuilder promptBuilder;
        private FinalAnswerPromptContextBuilder promptContextBuilder;
        private DefaultAgentFinalAnswerService service;
        private Prompt prompt;

        @BeforeEach
        void setUp() {
                chatClient = mock(ChatClient.class);
                requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
                callResponseSpec = mock(ChatClient.CallResponseSpec.class);
                streamResponseSpec = mock(ChatClient.StreamResponseSpec.class);
                promptBuilder = mock(PromptBuilder.class);
                promptContextBuilder = mock(FinalAnswerPromptContextBuilder.class);
                prompt = mock(Prompt.class);

                service = new DefaultAgentFinalAnswerService(
                                chatClient,
                                promptBuilder,
                                promptContextBuilder);
        }

        @Test
        void shouldGenerateFinalAnswer() {
                AgentContext context = AgentContext.from(
                                "分析劳动合同并生成律师意见");

                FinalAnswerPromptContext promptContext = promptContext();

                preparePrompt(context, promptContext);

                when(requestSpec.call())
                                .thenReturn(callResponseSpec);

                when(callResponseSpec.content())
                                .thenReturn("  根据现有材料，该劳动合同存在以下法律风险。  ");

                String result = service.generate(context);

                assertEquals(
                                "根据现有材料，该劳动合同存在以下法律风险。",
                                result);
        }

        @Test
        void shouldStreamChunksAndReturnCompleteAnswer() {
                AgentContext context = AgentContext.from(
                                "分析劳动合同并生成律师意见");

                FinalAnswerPromptContext promptContext = promptContext();

                preparePrompt(context, promptContext);

                when(requestSpec.stream())
                                .thenReturn(streamResponseSpec);

                when(streamResponseSpec.content())
                                .thenReturn(Flux.just(
                                                "劳动合同解除",
                                                "需要满足",
                                                "相应法定条件。"));

                List<String> chunks = new ArrayList<>();

                String result = service.stream(
                                context,
                                chunks::add);

                assertEquals(
                                "劳动合同解除需要满足相应法定条件。",
                                result);

                assertEquals(
                                List.of(
                                                "劳动合同解除",
                                                "需要满足",
                                                "相应法定条件。"),
                                chunks);

                verify(requestSpec).stream();
                verify(streamResponseSpec).content();
        }

        @Test
        void shouldIgnoreNullOrEmptyStreamChunks() {
                AgentContext context = AgentContext.from("分析劳动合同");
                preparePrompt(context, promptContext());

                when(requestSpec.stream())
                                .thenReturn(streamResponseSpec);

                when(streamResponseSpec.content())
                                .thenReturn(Flux.just("有效", "", "结果"));

                List<String> chunks = new ArrayList<>();

                String result = service.stream(context, chunks::add);

                assertEquals("有效结果", result);
                assertEquals(List.of("有效", "结果"), chunks);
        }

        @Test
        void shouldRejectNullStreamingConsumer() {
                assertThrows(
                                NullPointerException.class,
                                () -> service.stream(
                                                AgentContext.from("分析劳动合同"),
                                                null));
        }

        @Test
        void shouldRejectBlankStreamingResult() {
                AgentContext context = AgentContext.from("分析劳动合同");
                preparePrompt(context, promptContext());

                when(requestSpec.stream())
                                .thenReturn(streamResponseSpec);

                when(streamResponseSpec.content())
                                .thenReturn(Flux.just("   "));

                assertThrows(
                                IllegalStateException.class,
                                () -> service.stream(
                                                context,
                                                chunk -> {
                                                }));
        }

        @Test
        void shouldRejectNullContext() {
                assertThrows(
                                NullPointerException.class,
                                () -> service.generate(null));
        }

        @Test
        void shouldRejectNullFinalAnswer() {
                AgentContext context = AgentContext.from("分析劳动合同");
                preparePrompt(context, new FinalAnswerPromptContext(
                                "分析劳动合同",
                                null,
                                null,
                                null));

                when(requestSpec.call())
                                .thenReturn(callResponseSpec);

                when(callResponseSpec.content())
                                .thenReturn(null);

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> service.generate(context));

                assertEquals(
                                "Final answer must not be blank",
                                exception.getMessage());
        }

        @Test
        void shouldRejectBlankFinalAnswer() {
                AgentContext context = AgentContext.from("分析劳动合同");
                preparePrompt(context, new FinalAnswerPromptContext(
                                "分析劳动合同",
                                null,
                                null,
                                null));

                when(requestSpec.call())
                                .thenReturn(callResponseSpec);

                when(callResponseSpec.content())
                                .thenReturn("     ");

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> service.generate(context));

                assertEquals(
                                "Final answer must not be blank",
                                exception.getMessage());
        }

        private void preparePrompt(
                        AgentContext context,
                        FinalAnswerPromptContext promptContext) {

                when(promptContextBuilder.build(context))
                                .thenReturn(promptContext);

                when(promptBuilder.buildFinalAnswer(promptContext))
                                .thenReturn(prompt);

                when(chatClient.prompt(prompt))
                                .thenReturn(requestSpec);
        }

        private FinalAnswerPromptContext promptContext() {
                return new FinalAnswerPromptContext(
                                "分析劳动合同并生成律师意见",
                                "用户希望分析劳动合同风险",
                                "task-1 | COMPLETED | 查询法律依据",
                                "劳动合同法相关检索结果");
        }
}