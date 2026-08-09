package com.quince.lawyeraiassistant.prompt.builder;

import com.quince.lawyeraiassistant.agent.prompt.model.FinalAnswerPromptContext;
import com.quince.lawyeraiassistant.agent.prompt.model.ReflectionPromptContext;
import com.quince.lawyeraiassistant.agent.prompt.model.ReplanningPromptContext;
import com.quince.lawyeraiassistant.agent.prompt.model.RuntimeReasonPromptContext;
import com.quince.lawyeraiassistant.prompt.PromptNames;
import com.quince.lawyeraiassistant.prompt.PromptPaths;
import com.quince.lawyeraiassistant.prompt.factory.PromptFactory;
import com.quince.lawyeraiassistant.prompt.knowledge.DefaultKnowledgeFormatter;
import com.quince.lawyeraiassistant.prompt.knowledge.KnowledgeFormatter;
import com.quince.lawyeraiassistant.prompt.model.PromptContext;
import com.quince.lawyeraiassistant.prompt.model.PromptFragment;
import com.quince.lawyeraiassistant.prompt.template.DefaultTemplateRenderer;
import com.quince.lawyeraiassistant.prompt.template.TemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromptBuilderTest {

        private PromptFactory promptFactory;

        private TemplateRenderer templateRenderer;

        private KnowledgeFormatter knowledgeFormatter;

        private PromptBuilder promptBuilder;

        @BeforeEach
        void setUp() {
                promptFactory = mock(PromptFactory.class);

                templateRenderer = new DefaultTemplateRenderer();

                knowledgeFormatter = new DefaultKnowledgeFormatter();

                promptBuilder = new PromptBuilder(
                                promptFactory,
                                templateRenderer,
                                knowledgeFormatter);
        }

        @Test
        void shouldBuildPromptWithSystemAndUserMessages() {
                when(promptFactory.lawyerSystem())
                                .thenReturn(
                                                createFragment(
                                                                """
                                                                                你是一名专业律师。

                                                                                参考知识：
                                                                                {knowledge}

                                                                                用户问题：
                                                                                {question}
                                                                                """));

                PromptContext context = createContext(
                                "劳动合同到期是否需要补偿？",
                                List.of());

                Prompt prompt = promptBuilder.buildLegal(context);

                List<Message> messages = prompt.getInstructions();

                assertEquals(2, messages.size());

                assertInstanceOf(
                                SystemMessage.class,
                                messages.get(0));

                assertInstanceOf(
                                UserMessage.class,
                                messages.get(1));
        }

        @Test
        void shouldInjectFormattedKnowledgeIntoSystemMessage() {
                when(promptFactory.lawyerSystem())
                                .thenReturn(
                                                createFragment(
                                                                """
                                                                                参考法律知识：

                                                                                {knowledge}

                                                                                用户问题：

                                                                                {question}
                                                                                """));

                Document document = Document.builder()
                                .text(
                                                "第八十七条规定，用人单位违法解除劳动合同的，"
                                                                + "应按照经济补偿标准的二倍支付赔偿金。")
                                .metadata(
                                                Map.of(
                                                                "file_name",
                                                                "劳动合同法.pdf",
                                                                "page_number",
                                                                24,
                                                                "chunk_index",
                                                                0))
                                .score(0.91)
                                .build();

                PromptContext context = createContext(
                                "违法解除劳动合同如何赔偿？",
                                List.of(document));

                Prompt prompt = promptBuilder.buildLegal(context);

                SystemMessage systemMessage = getSystemMessage(prompt);

                String content = systemMessage.getText();

                assertTrue(
                                content.contains(
                                                "## 参考资料 1"));

                assertTrue(
                                content.contains(
                                                "劳动合同法.pdf"));

                assertTrue(
                                content.contains(
                                                "第八十七条规定"));

                assertTrue(
                                content.contains(
                                                "违法解除劳动合同如何赔偿？"));

                assertFalse(
                                content.contains("{knowledge}"));

                assertFalse(
                                content.contains("{question}"));
        }

        @Test
        void shouldRenderEmptyKnowledgeWhenNoDocumentsExist() {
                when(promptFactory.lawyerSystem())
                                .thenReturn(
                                                createFragment(
                                                                """
                                                                                参考知识：
                                                                                {knowledge}

                                                                                问题：
                                                                                {question}
                                                                                """));

                PromptContext context = createContext(
                                "劳动合同解除需要赔偿吗？",
                                List.of());

                Prompt prompt = promptBuilder.buildLegal(context);

                SystemMessage systemMessage = getSystemMessage(prompt);

                assertTrue(
                                systemMessage.getText()
                                                .contains("参考知识："));

                assertFalse(
                                systemMessage.getText()
                                                .contains("{knowledge}"));
        }

        @Test
        void shouldPreserveRetrieverDocumentOrder() {
                when(promptFactory.lawyerSystem())
                                .thenReturn(
                                                createFragment(
                                                                "{knowledge}"));

                Document firstDocument = Document.builder()
                                .text(
                                                "第四十六条：经济补偿适用情形。")
                                .metadata(
                                                Map.of(
                                                                "file_name",
                                                                "劳动合同法.pdf"))
                                .score(0.95)
                                .build();

                Document secondDocument = Document.builder()
                                .text(
                                                "第四十七条：经济补偿计算标准。")
                                .metadata(
                                                Map.of(
                                                                "file_name",
                                                                "劳动合同法.pdf"))
                                .score(0.90)
                                .build();

                PromptContext context = createContext(
                                "经济补偿如何计算？",
                                List.of(
                                                firstDocument,
                                                secondDocument));

                Prompt prompt = promptBuilder.buildLegal(context);

                String content = getSystemMessage(prompt).getText();

                int firstPosition = content.indexOf("第四十六条");

                int secondPosition = content.indexOf("第四十七条");

                assertTrue(firstPosition >= 0);
                assertTrue(secondPosition > firstPosition);
        }

        @Test
        void shouldUseQuestionAsUserMessage() {
                when(promptFactory.lawyerSystem())
                                .thenReturn(
                                                createFragment(
                                                                "你是一名专业律师。"));

                PromptContext context = createContext(
                                "未签劳动合同怎么办？",
                                List.of());

                Prompt prompt = promptBuilder.buildLegal(context);

                UserMessage userMessage = getUserMessage(prompt);

                assertEquals(
                                "未签劳动合同怎么办？",
                                userMessage.getText());
        }

        @Test
        void shouldThrowExceptionWhenContextIsNull() {
                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> promptBuilder.buildLegal(null));

                assertEquals(
                                "PromptContext must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenQuestionIsBlank() {
                PromptContext context = PromptContext.builder()
                                .question("   ")
                                .build();

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> promptBuilder.buildLegal(context));

                assertEquals(
                                "Prompt question must not be blank",
                                exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenSystemFragmentIsNull() {
                when(promptFactory.lawyerSystem())
                                .thenReturn(null);

                PromptContext context = createContext(
                                "劳动合同问题",
                                List.of());

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> promptBuilder.buildLegal(context));

                assertEquals(
                                "Lawyer System PromptFragment must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullConstructorDependencies() {
                assertThrows(
                                NullPointerException.class,
                                () -> new PromptBuilder(
                                                null,
                                                templateRenderer,
                                                knowledgeFormatter));

                assertThrows(
                                NullPointerException.class,
                                () -> new PromptBuilder(
                                                promptFactory,
                                                null,
                                                knowledgeFormatter));

                assertThrows(
                                NullPointerException.class,
                                () -> new PromptBuilder(
                                                promptFactory,
                                                templateRenderer,
                                                null));
        }

        @Test
        void shouldBuildFinalAnswerPrompt() {

                when(promptFactory.agentFinalAnswer())
                                .thenReturn(
                                                createFragment(
                                                                """
                                                                                Goal:
                                                                                {goal}

                                                                                Reason summary:
                                                                                {reasonSummary}

                                                                                Plan:
                                                                                {plan}

                                                                                Observations:
                                                                                {observations}
                                                                                """));

                FinalAnswerPromptContext context = new FinalAnswerPromptContext(
                                "分析劳动合同",
                                "用户希望分析劳动合同风险",
                                "task-1 | COMPLETED | 查询法律依据",
                                "劳动合同法相关检索结果");

                Prompt prompt = promptBuilder.buildFinalAnswer(
                                context);

                assertNotNull(
                                prompt);

                String content = prompt.getInstructions()
                                .getFirst()
                                .getText();

                assertTrue(
                                content.contains(
                                                "分析劳动合同"));

                assertTrue(
                                content.contains(
                                                "用户希望分析劳动合同风险"));

                assertTrue(
                                content.contains(
                                                "劳动合同法相关检索结果"));
        }

        @Test
        void shouldBuildReflectionPrompt() {

                ReflectionPromptContext context = new ReflectionPromptContext(
                                "分析劳动合同",
                                "用户希望分析合同风险",
                                "task-1",
                                "查询相关法律规定",
                                "task-1 | RUNNING | 查询相关法律规定",
                                "已经检索到劳动合同法相关内容");

                PromptFragment reflectionFragment = PromptFragment.builder()
                                .name(PromptNames.AGENT_REFLECTION)
                                .content("""
                                                Goal:
                                                {goal}

                                                Task:
                                                {taskDescription}

                                                Observations:
                                                {observations}
                                                """)
                                .version("v1")
                                .source(PromptPaths.AGENT_REFLECTION)
                                .build();

                when(
                                promptFactory.agentReflection())
                                .thenReturn(
                                                reflectionFragment);

                Prompt prompt = promptBuilder.buildReflection(
                                context);

                assertNotNull(
                                prompt);

                String text = prompt.getInstructions()
                                .getFirst()
                                .getText();

                assertTrue(
                                text.contains(
                                                "分析劳动合同"));

                assertTrue(
                                text.contains(
                                                "查询相关法律规定"));

                assertTrue(
                                text.contains(
                                                "劳动合同法相关内容"));
        }

        @Test
        void shouldBuildReplanningPrompt() {

                ReplanningPromptContext context = new ReplanningPromptContext(
                                "分析劳动合同",
                                "分析合同法律风险",
                                "task-1 | COMPLETED | 查询法律依据",
                                "已经获得相关法律依据",
                                "原计划没有覆盖赔偿责任");

                PromptFragment replanningFragment = PromptFragment.builder()
                                .name(PromptNames.AGENT_REPLANNING)
                                .content("""
                                                Goal:
                                                {goal}

                                                Reason summary:
                                                {reasonSummary}

                                                Current plan:
                                                {currentPlan}

                                                Observations:
                                                {observations}

                                                Reflection summary:
                                                {reflectionSummary}
                                                """)
                                .version("v1")
                                .source(PromptPaths.AGENT_REPLANNING)
                                .build();

                when(
                                promptFactory.agentReplanning())
                                .thenReturn(
                                                replanningFragment);

                Prompt prompt = promptBuilder.buildReplanning(
                                context);

                assertNotNull(
                                prompt);

                verify(
                                promptFactory)
                                .agentReplanning();
        }

        @Test
        void shouldBuildRuntimeReasonPrompt() {

                RuntimeReasonPromptContext context = new RuntimeReasonPromptContext(
                                "分析劳动合同",
                                "task-2 | RUNNING | 分析竞业限制条款",
                                "task-1 | COMPLETED | 读取劳动合同",
                                "合同约定竞业限制24个月");

                PromptFragment runtimeReasonFragment = PromptFragment.builder()
                                .name(PromptNames.AGENT_RUNTIME_REASON)
                                .content("""
                                                Goal:
                                                {goal}

                                                Current task:
                                                {currentTask}

                                                Current plan:
                                                {currentPlan}

                                                Observations:
                                                {observations}
                                                """)
                                .version("v1")
                                .source(PromptPaths.AGENT_RUNTIME_REASON)
                                .build();

                when(promptFactory.agentRuntimeReason())
                                .thenReturn(runtimeReasonFragment);

                Prompt prompt = promptBuilder.buildRuntimeReason(context);

                assertNotNull(prompt);
                assertEquals(1, prompt.getInstructions().size());

                SystemMessage message = assertInstanceOf(
                                SystemMessage.class,
                                prompt.getInstructions().getFirst());

                String content = message.getText();

                assertTrue(content.contains("分析劳动合同"));
                assertTrue(content.contains("task-2 | RUNNING | 分析竞业限制条款"));
                assertTrue(content.contains(
                                "task-1 | COMPLETED | 读取劳动合同"));
                assertTrue(content.contains(
                                "合同约定竞业限制24个月"));

                verify(promptFactory).agentRuntimeReason();
        }

        private PromptContext createContext(
                        String question,
                        List<Document> documents) {
                return PromptContext.builder()
                                .question(question)
                                .knowledge(documents)
                                .conversationId(
                                                "conversation-001")
                                .build();
        }

        private PromptFragment createFragment(
                        String content) {
                return PromptFragment.builder()
                                .name("lawyer-system")
                                .content(content)
                                .version("v1")
                                .source(
                                                "classpath:prompts/system/lawyer-system.st")
                                .build();
        }

        private SystemMessage getSystemMessage(
                        Prompt prompt) {
                Message message = prompt.getInstructions().get(0);

                return assertInstanceOf(
                                SystemMessage.class,
                                message);
        }

        private UserMessage getUserMessage(
                        Prompt prompt) {
                Message message = prompt.getInstructions().get(1);

                return assertInstanceOf(
                                UserMessage.class,
                                message);
        }
}
