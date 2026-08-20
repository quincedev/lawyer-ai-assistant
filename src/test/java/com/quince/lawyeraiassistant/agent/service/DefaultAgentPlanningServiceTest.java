package com.quince.lawyeraiassistant.agent.service;

import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ReasonResult;
import com.quince.lawyeraiassistant.agent.parser.AgentPlanParser;
import com.quince.lawyeraiassistant.agent.prompt.model.PlanningPromptContext;
import com.quince.lawyeraiassistant.agent.service.support.BoundedLlmCallExecutor;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAgentPlanningServiceTest {

        private ChatClient chatClient;

        private PromptBuilder promptBuilder;

        private AgentPlanParser agentPlanParser;

        private ChatClientRequestSpec requestSpec;

        private CallResponseSpec responseSpec;

        private DefaultAgentPlanningService planningService;

        private BoundedLlmCallExecutor llmCallExecutor;

        @BeforeEach
        void setUp() {

                chatClient = mock(
                                ChatClient.class);

                promptBuilder = mock(
                                PromptBuilder.class);

                agentPlanParser = mock(
                                AgentPlanParser.class);

                requestSpec = mock(
                                ChatClientRequestSpec.class);

                responseSpec = mock(
                                CallResponseSpec.class);

                llmCallExecutor = new BoundedLlmCallExecutor();

                planningService = new DefaultAgentPlanningService(
                                chatClient,
                                promptBuilder,
                                agentPlanParser,
                                llmCallExecutor);
        }

        @Test
        void shouldGenerateAgentPlan() {

                PlanningPromptContext context = PlanningPromptContext.from(
                                "分析劳动合同并生成律师意见书",
                                ReasonResult.from(
                                                "用户希望分析劳动合同并生成律师意见书。"),
                                "优先检索法律知识库",
                                "searchLegalKnowledge");

                Prompt prompt = mock(
                                Prompt.class);

                String content = """
                                task-1|读取劳动合同
                                task-2|识别法律风险
                                task-3|生成律师意见书
                                """;

                AgentPlan expectedPlan = AgentPlan.from(
                                List.of(
                                                AgentTask.pending(
                                                                "task-1",
                                                                "读取劳动合同"),
                                                AgentTask.pending(
                                                                "task-2",
                                                                "识别法律风险"),
                                                AgentTask.pending(
                                                                "task-3",
                                                                "生成律师意见书")));

                when(
                                promptBuilder.buildPlanning(
                                                context))
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

                AgentPlan result = planningService.plan(
                                context);

                assertSame(
                                expectedPlan,
                                result);

                verify(
                                promptBuilder)
                                .buildPlanning(
                                                context);

                verify(
                                chatClient)
                                .prompt(
                                                prompt);

                verify(
                                requestSpec)
                                .call();

                verify(
                                responseSpec)
                                .content();

                verify(
                                agentPlanParser)
                                .parse(
                                                content);
        }

        @Test
        void shouldSupportPlanningWithoutSkillAndTools() {

                PlanningPromptContext context = PlanningPromptContext.from(
                                "分析人工智能 Agent 和传统 Workflow 的主要区别",
                                ReasonResult.from(
                                                "用户希望比较两种技术模式。"),
                                "无",
                                "无");

                Prompt prompt = mock(
                                Prompt.class);

                String content = """
                                task-1|梳理人工智能Agent的核心特征
                                task-2|梳理传统Workflow的核心特征
                                task-3|比较两者的主要区别
                                """;

                AgentPlan expectedPlan = AgentPlan.from(
                                List.of(
                                                AgentTask.pending(
                                                                "task-1",
                                                                "梳理人工智能Agent的核心特征"),
                                                AgentTask.pending(
                                                                "task-2",
                                                                "梳理传统Workflow的核心特征"),
                                                AgentTask.pending(
                                                                "task-3",
                                                                "比较两者的主要区别")));

                when(
                                promptBuilder.buildPlanning(
                                                context))
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

                AgentPlan result = planningService.plan(
                                context);

                assertSame(
                                expectedPlan,
                                result);

                assertEquals(
                                "无",
                                context.getSkillInstructions());

                assertEquals(
                                "无",
                                context.getAvailableTools());

                verify(
                                promptBuilder)
                                .buildPlanning(
                                                context);

                verify(
                                agentPlanParser)
                                .parse(
                                                content);
        }

        @Test
        void shouldRejectNullContext() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> planningService.plan(
                                                null));

                assertEquals(
                                "PlanningPromptContext must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectBlankPlanningResult() {

                PlanningPromptContext context = createContext();

                Prompt prompt = mock(
                                Prompt.class);

                when(
                                promptBuilder.buildPlanning(
                                                context))
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
                                () -> planningService.plan(
                                                context));

                assertEquals(
                                "Planning result must not be blank",
                                exception.getMessage());

                verify(
                                responseSpec,
                                times(2))
                                .content();
        }

        @Test
        void shouldRejectNullPlanningResult() {

                PlanningPromptContext context = createContext();

                Prompt prompt = mock(
                                Prompt.class);

                when(
                                promptBuilder.buildPlanning(
                                                context))
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
                                () -> planningService.plan(
                                                context));

                assertEquals(
                                "Planning result must not be blank",
                                exception.getMessage());
        }

        @Test
        void shouldRetryPlanningOnceWhenFirstResponseIsBlank() {

                PlanningPromptContext context = createContext();

                Prompt prompt = mock(
                                Prompt.class);

                String validContent = """
                                task-1|识别违法解除争议点
                                task-2|检索相关法律依据
                                task-3|形成法律结论
                                """;

                AgentPlan expectedPlan = AgentPlan.from(
                                List.of(
                                                AgentTask.pending(
                                                                "task-1",
                                                                "识别违法解除争议点"),
                                                AgentTask.pending(
                                                                "task-2",
                                                                "检索相关法律依据"),
                                                AgentTask.pending(
                                                                "task-3",
                                                                "形成法律结论")));

                when(
                                promptBuilder.buildPlanning(
                                                context))
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
                                                "   ",
                                                validContent);

                when(
                                agentPlanParser.parse(
                                                validContent))
                                .thenReturn(
                                                expectedPlan);

                AgentPlan result = planningService.plan(
                                context);

                assertSame(
                                expectedPlan,
                                result);

                verify(
                                responseSpec,
                                org.mockito.Mockito.times(
                                                2))
                                .content();

                verify(
                                agentPlanParser)
                                .parse(
                                                validContent);
        }

        private PlanningPromptContext createContext() {

                return PlanningPromptContext.from(
                                "分析劳动合同",
                                ReasonResult.from(
                                                "用户希望分析劳动合同。"),
                                "无",
                                "无");
        }
}