package com.quince.lawyeraiassistant.agent.action;

import com.quince.lawyeraiassistant.agent.action.policy.EvidenceAwareActionPolicy;
import com.quince.lawyeraiassistant.agent.model.AgentAction;
import com.quince.lawyeraiassistant.agent.model.AgentActionDecision;
import com.quince.lawyeraiassistant.agent.model.AgentActionType;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;
import com.quince.lawyeraiassistant.agent.prompt.config.AgentPromptWindowProperties;
import com.quince.lawyeraiassistant.agent.skill.AgentSkill;
import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;
import com.quince.lawyeraiassistant.agent.skill.scope.SkillToolScope;
import com.quince.lawyeraiassistant.agent.tool.AgentTool;
import com.quince.lawyeraiassistant.agent.tool.AgentToolRegistry;
import com.quince.lawyeraiassistant.security.legal.evidence.LegalEvidencePromptFormatter;
import com.quince.lawyeraiassistant.security.SecurityTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.quince.lawyeraiassistant.security.legal.TestLegalSecurityContexts.toolResult;

class SpringAiAgentActionSelectorTest {

        private ChatClient.Builder chatClientBuilder;

        private ChatClient chatClient;

        private ChatClient.ChatClientRequestSpec requestSpec;

        private ChatClient.PromptUserSpec promptUserSpec;

        private ChatClient.CallResponseSpec callResponseSpec;

        private AgentActionDecisionMapper decisionMapper;

        private AgentToolRegistry toolRegistry;

        private SkillToolScope skillToolScope;

        private Resource promptResource;

        private SpringAiAgentActionSelector selector;

        private LegalEvidencePromptFormatter evidencePromptFormatter;

        private AgentPromptWindowProperties promptWindowProperties;

        @BeforeEach
        void setUp() {

                chatClientBuilder = mock(
                                ChatClient.Builder.class);

                chatClient = mock(
                                ChatClient.class);

                requestSpec = mock(
                                ChatClient.ChatClientRequestSpec.class);

                promptUserSpec = mock(
                                ChatClient.PromptUserSpec.class);

                callResponseSpec = mock(
                                ChatClient.CallResponseSpec.class);

                decisionMapper = new AgentActionDecisionMapper();

                skillToolScope = mock(
                                SkillToolScope.class);

                promptResource = mock(
                                Resource.class);

                AgentTool legalTool = mockTool(
                                "searchLegalKnowledge");

                AgentTool customerTool = mockTool(
                                "queryCustomer");

                evidencePromptFormatter = new LegalEvidencePromptFormatter();

                promptWindowProperties = new AgentPromptWindowProperties();

                toolRegistry = new AgentToolRegistry(
                                List.of(
                                                legalTool,
                                                customerTool));

                when(
                                chatClientBuilder.build())
                                .thenReturn(
                                                chatClient);

                when(
                                chatClient.prompt())
                                .thenReturn(
                                                requestSpec);

                when(
                                requestSpec.user(
                                                org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any()))
                                .thenAnswer(
                                                invocation -> {
                                                        Consumer<ChatClient.PromptUserSpec> userSpecConsumer = invocation
                                                                        .getArgument(0);

                                                        userSpecConsumer.accept(
                                                                        promptUserSpec);

                                                        return requestSpec;
                                                });

                when(
                                promptUserSpec.text(
                                                any(Resource.class)))
                                .thenReturn(
                                                promptUserSpec);

                when(
                                promptUserSpec.param(
                                                any(String.class),
                                                any()))
                                .thenReturn(
                                                promptUserSpec);

                when(
                                requestSpec.call())
                                .thenReturn(
                                                callResponseSpec);

                when(
                                callResponseSpec.entity(
                                                AgentActionDecision.class))
                                .thenReturn(
                                                new AgentActionDecision(
                                                                AgentActionType.REASON,
                                                                null,
                                                                null));

                selector = new SpringAiAgentActionSelector(
                                chatClientBuilder,
                                decisionMapper,
                                toolRegistry,
                                skillToolScope,
                                evidencePromptFormatter,
                                promptWindowProperties,
                                new EvidenceAwareActionPolicy(),
                                promptResource);
        }

        @Test
        void shouldApplySkillToolScopeWhenSelectingAction() {

                AgentSkill skill = AgentSkill.of(
                                "legal-research",
                                "Legal Research",
                                "用于研究具体法律问题",
                                "执行法律研究",
                                List.of(
                                                "searchLegalKnowledge"),
                                Set.of(
                                                "legal",
                                                "research"));

                AgentContext context = AgentContext.from(
                                "研究违法解除劳动合同")
                                .withSkillContext(
                                                SkillContext.of(
                                                                skill));

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "查询违法解除的法律责任");

                when(
                                skillToolScope.filterAllowed(
                                                context.getSkillContext(),
                                                toolRegistry.names()))
                                .thenReturn(
                                                List.of(
                                                                "searchLegalKnowledge"));

                AgentAction result = selector.select(
                                context,
                                task);

                assertNotNull(
                                result);

                verify(
                                promptUserSpec)
                                .param(
                                                "availableTools",
                                                "searchLegalKnowledge");
        }

        @Test
        void shouldExposeNoToolsWhenNoSkillSelected() {

                AgentContext context = AgentContext.from(
                                "处理用户问题");

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "处理当前问题");

                when(
                                skillToolScope.filterAllowed(
                                                context.getSkillContext(),
                                                toolRegistry.names()))
                                .thenReturn(
                                                List.of());

                AgentAction result = selector.select(
                                context,
                                task);

                assertNotNull(
                                result);

                verify(
                                skillToolScope)
                                .filterAllowed(
                                                context.getSkillContext(),
                                                toolRegistry.names());

                verify(
                                promptUserSpec)
                                .param(
                                                "availableTools",
                                                "无");
        }

        @Test
        void shouldSupportSkillWithNoAvailableTools() {

                AgentSkill skill = AgentSkill.of(
                                "legal-summary",
                                "Legal Summary",
                                "用于总结已有法律材料",
                                "根据已有上下文进行总结",
                                List.of(),
                                Set.of(
                                                "legal",
                                                "summary"));

                AgentContext context = AgentContext.from(
                                "总结已有法律材料")
                                .withSkillContext(
                                                SkillContext.of(
                                                                skill));

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "总结现有材料");

                when(
                                skillToolScope.filterAllowed(
                                                context.getSkillContext(),
                                                toolRegistry.names()))
                                .thenReturn(
                                                List.of());

                AgentAction result = selector.select(
                                context,
                                task);

                assertNotNull(
                                result);

                verify(
                                skillToolScope)
                                .filterAllowed(
                                                context.getSkillContext(),
                                                toolRegistry.names());
        }

        @Test
        void shouldInjectSkillIntoActionSelectionPrompt() {

                AgentSkill skill = AgentSkill.of(
                                "legal-research",
                                "Legal Research",
                                "用于研究具体法律问题",
                                "优先检索法律知识库，并基于检索结果进行法律分析",
                                List.of(
                                                "searchLegalKnowledge"),
                                Set.of(
                                                "legal",
                                                "research"));

                AgentContext context = AgentContext.from(
                                "研究违法解除劳动合同")
                                .withSkillContext(
                                                SkillContext.of(
                                                                skill));

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "查询违法解除的法律责任");

                when(
                                skillToolScope.filterAllowed(
                                                context.getSkillContext(),
                                                toolRegistry.names()))
                                .thenReturn(
                                                List.of(
                                                                "searchLegalKnowledge"));

                selector.select(
                                context,
                                task);

                verify(
                                promptUserSpec)
                                .param(
                                                "skillName",
                                                "Legal Research");

                verify(
                                promptUserSpec)
                                .param(
                                                "skillInstructions",
                                                "优先检索法律知识库，并基于检索结果进行法律分析");
        }

        @Test
        void shouldUseNoneSkillPromptValuesWhenNoSkillSelected() {

                AgentContext context = AgentContext.from(
                                "处理用户问题");

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "处理当前问题");

                when(
                                skillToolScope.filterAllowed(
                                                context.getSkillContext(),
                                                toolRegistry.names()))
                                .thenReturn(
                                                List.of());

                selector.select(
                                context,
                                task);

                verify(
                                promptUserSpec)
                                .param(
                                                "skillName",
                                                "无");

                verify(
                                promptUserSpec)
                                .param(
                                                "skillInstructions",
                                                "无");

                verify(
                                promptUserSpec)
                                .param(
                                                "availableTools",
                                                "无");
        }

        @Test
        void shouldBuildLegalResearchDecisionContextBeforeResearch() {

                AgentSkill skill = AgentSkill.of(
                                "legal-research",
                                "Legal Research",
                                "用于研究具体法律问题",
                                "对需要法律依据支持的判断，应优先检索法律知识库",
                                List.of(
                                                "searchLegalKnowledge"),
                                Set.of(
                                                "legal",
                                                "research"));

                AgentContext context = AgentContext.from(
                                "研究用人单位违法解除劳动合同需要承担什么法律责任")
                                .withSkillContext(
                                                SkillContext.of(
                                                                skill));

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "检索违法解除劳动合同的法律依据");

                when(
                                skillToolScope.filterAllowed(
                                                context.getSkillContext(),
                                                toolRegistry.names()))
                                .thenReturn(
                                                List.of(
                                                                "searchLegalKnowledge"));

                selector.select(
                                context,
                                task);

                verify(
                                promptUserSpec)
                                .param(
                                                "skillName",
                                                "Legal Research");

                verify(
                                promptUserSpec)
                                .param(
                                                "skillInstructions",
                                                "对需要法律依据支持的判断，应优先检索法律知识库");

                verify(
                                promptUserSpec)
                                .param(
                                                "observations",
                                                "无");

                verify(
                                promptUserSpec)
                                .param(
                                                "availableTools",
                                                "searchLegalKnowledge");
        }

        @SecurityTest
        @Test
        void shouldExposeSuccessfulLegalResearchObservationToActionSelection() {

                AgentSkill skill = AgentSkill.of(
                                "legal-research",
                                "Legal Research",
                                "用于研究具体法律问题",
                                "对需要法律依据支持的判断，应优先检索法律知识库，并基于检索结果进行分析",
                                List.of(
                                                "searchLegalKnowledge"),
                                Set.of(
                                                "legal",
                                                "research"));

                ToolObservation observation = ToolObservation.success(
                                "task-1",
                                "searchLegalKnowledge",
                                "《中华人民共和国劳动合同法》第八十七条规定："
                                                + "用人单位违反本法规定解除或者终止劳动合同的，"
                                                + "应当依照经济补偿标准的二倍向劳动者支付赔偿金。",
                                toolResult());

                AgentContext context = AgentContext.from(
                                "研究用人单位违法解除劳动合同需要承担什么法律责任")
                                .withSkillContext(
                                                SkillContext.of(
                                                                skill))
                                .appendObservation(
                                                observation);

                AgentTask task = AgentTask.pending(
                                "task-2",
                                "根据已检索的法律依据分析违法解除的法律责任");

                when(
                                skillToolScope.filterAllowed(
                                                context.getSkillContext(),
                                                toolRegistry.names()))
                                .thenReturn(
                                                List.of(
                                                                "searchLegalKnowledge"));

                AgentAction result = selector.select(
                                context,
                                task);

                assertNotNull(
                                result);

                verify(
                                promptUserSpec)
                                .param(
                                                "skillName",
                                                "Legal Research");

                verify(
                                promptUserSpec)
                                .param(
                                                "skillInstructions",
                                                "对需要法律依据支持的判断，应优先检索法律知识库，并基于检索结果进行分析");

                verify(
                                promptUserSpec)
                                .param(
                                                "availableTools",
                                                "searchLegalKnowledge");

                verify(
                                promptUserSpec)
                                .param(
                                                org.mockito.ArgumentMatchers.eq("observations"),
                                                org.mockito.ArgumentMatchers.argThat(
                                                                value -> {
                                                                        String observations = String.valueOf(
                                                                                        value);

                                                                        return observations.contains(
                                                                                        "Source: TOOL_RESULT")
                                                                                        && observations.contains(
                                                                                                        "Trust-Level: UNTRUSTED")
                                                                                        && observations.contains(
                                                                                                        "Interpretation: DATA_ONLY")
                                                                                        && observations.contains(
                                                                                                        "<UNTRUSTED_EVIDENCE>")
                                                                                        && observations.contains(
                                                                                                        "劳动合同法");
                                                                }));
        }

        @Test
        void shouldExposeFailedLegalResearchObservationToActionSelection() {

                AgentSkill skill = AgentSkill.of(
                                "legal-research",
                                "Legal Research",
                                "用于研究具体法律问题",
                                "对需要法律依据支持的判断，应优先检索法律知识库",
                                List.of("searchLegalKnowledge"),
                                Set.of("legal", "research"));

                ToolObservation observation = ToolObservation.failure(
                                "task-1",
                                "searchLegalKnowledge",
                                "法律知识库检索失败",
                                toolResult());

                AgentContext context = AgentContext.from(
                                "研究违法解除劳动合同的法律责任")
                                .withSkillContext(
                                                SkillContext.of(skill))
                                .appendObservation(observation);

                AgentTask task = AgentTask.pending(
                                "task-1",
                                "检索违法解除劳动合同法律依据");

                when(
                                skillToolScope.filterAllowed(
                                                context.getSkillContext(),
                                                toolRegistry.names()))
                                .thenReturn(
                                                List.of("searchLegalKnowledge"));

                selector.select(
                                context,
                                task);

                verify(promptUserSpec)
                                .param(
                                                "skillName",
                                                "Legal Research");

                verify(promptUserSpec)
                                .param(
                                                org.mockito.ArgumentMatchers.eq("observations"),
                                                org.mockito.ArgumentMatchers.argThat(
                                                                value -> {
                                                                        String observations = String.valueOf(
                                                                                        value);

                                                                        return observations.contains(
                                                                                        "[OBSERVATION]")
                                                                                        && observations.contains(
                                                                                                        "Status: FAILED")
                                                                                        && observations.contains(
                                                                                                        "Source: TOOL_RESULT")
                                                                                        && observations.contains(
                                                                                                        "Trust-Level: UNTRUSTED")
                                                                                        && observations.contains(
                                                                                                        "法律知识库检索失败");
                                                                }));

                verify(promptUserSpec)
                                .param(
                                                "availableTools",
                                                "searchLegalKnowledge");
        }

        @Test
        void shouldLimitHistoricalObservationsInActionSelectionPrompt() {

                promptWindowProperties.setMaxHistoricalObservations(1);

                AgentContext context = AgentContext.from("Analyze observations")
                                .appendObservation(ToolObservation.success(
                                                "task-1",
                                                "searchLegalKnowledge",
                                                "HISTORICAL_A",
                                                toolResult()))
                                .appendObservation(ToolObservation.success(
                                                "task-2",
                                                "searchLegalKnowledge",
                                                "HISTORICAL_B",
                                                toolResult()));

                when(skillToolScope.filterAllowed(
                                context.getSkillContext(),
                                toolRegistry.names()))
                                .thenReturn(List.of());

                selector.select(
                                context,
                                AgentTask.pending("task-3", "Analyze current task"));

                verify(promptUserSpec).param(
                                org.mockito.ArgumentMatchers.eq("observations"),
                                org.mockito.ArgumentMatchers.argThat(value -> {
                                        String observations = String.valueOf(value);
                                        return observations.contains("HISTORICAL_B")
                                                        && !observations.contains("HISTORICAL_A");
                                }));
        }

        @Test
        void shouldExposeReasonPreferenceWhenAnalyticalTaskHasExistingEvidence() {

                AgentContext context = AgentContext.from("Analyze legal evidence")
                                .appendObservation(ToolObservation.success(
                                                "task-1",
                                                "searchLegalKnowledge",
                                                "EXISTING_EVIDENCE",
                                                toolResult()));

                when(skillToolScope.filterAllowed(
                                context.getSkillContext(),
                                toolRegistry.names()))
                                .thenReturn(List.of("searchLegalKnowledge"));

                selector.select(
                                context,
                                AgentTask.pending("task-2", "分析已有证据并形成结论"));

                verify(promptUserSpec).param(
                                org.mockito.ArgumentMatchers.eq("actionPolicyHint"),
                                org.mockito.ArgumentMatchers.argThat(
                                                value -> String.valueOf(value)
                                                                .contains("优先使用 REASON")));
        }

        private AgentTool mockTool(
                        String toolName) {

                AgentTool tool = mock(
                                AgentTool.class);

                when(
                                tool.name())
                                .thenReturn(
                                                toolName);

                return tool;
        }
}
