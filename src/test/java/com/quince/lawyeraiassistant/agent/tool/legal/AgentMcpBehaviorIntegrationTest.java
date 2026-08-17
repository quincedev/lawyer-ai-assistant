package com.quince.lawyeraiassistant.agent.tool.legal;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;

import com.quince.lawyeraiassistant.agent.skill.AgentSkill;
import com.quince.lawyeraiassistant.agent.skill.context.SkillContext;
import com.quince.lawyeraiassistant.agent.skill.scope.SkillToolScope;

import com.quince.lawyeraiassistant.agent.tool.AgentToolRegistry;
import com.quince.lawyeraiassistant.agent.tool.DefaultToolActionExecutor;
import com.quince.lawyeraiassistant.security.audit.SecurityAuditLogger;
import com.quince.lawyeraiassistant.security.legal.LegalSecurityContext;
import com.quince.lawyeraiassistant.security.legal.SecuritySource;
import com.quince.lawyeraiassistant.security.legal.SecurityTrustLevel;
import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityResult;
import com.quince.lawyeraiassistant.security.mcp.result.McpToolResultSecurityService;
import com.quince.lawyeraiassistant.security.runtime.AgentExecutionLimits;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentMcpBehaviorIntegrationTest {

        private ToolCallback mcpToolCallback;

        private AgentToolRegistry toolRegistry;

        private SkillToolScope skillToolScope;

        private DefaultToolActionExecutor toolActionExecutor;

        private AgentExecutionLimits executionLimits;

        private SkillContext legalSkillContext;

        private SecurityAuditLogger securityAuditLogger;

        @BeforeEach
        void setUp() {

                SyncMcpToolCallbackProvider provider = mock(
                                SyncMcpToolCallbackProvider.class);

                mcpToolCallback = mock(
                                ToolCallback.class);

                ToolDefinition definition = mock(
                                ToolDefinition.class);

                securityAuditLogger = mock(SecurityAuditLogger.class);

                when(
                                definition.name())
                                .thenReturn(
                                                LegalKnowledgeTool.TOOL_NAME);

                when(
                                mcpToolCallback.getToolDefinition())
                                .thenReturn(
                                                definition);

                when(
                                provider.getToolCallbacks())
                                .thenReturn(
                                                new ToolCallback[] {
                                                                mcpToolCallback
                                                });

                McpToolResultSecurityService resultSecurityService = mock(
                                McpToolResultSecurityService.class);

                when(
                                resultSecurityService.evaluate(
                                                anyString(),
                                                anyString()))
                                .thenAnswer(
                                                invocation -> McpToolResultSecurityResult.allow(
                                                                invocation.getArgument(0),
                                                                "testResultSecurity"));

                McpLegalKnowledgeTool mcpTool = new McpLegalKnowledgeTool(
                                provider,
                                new ObjectMapper(),
                                resultSecurityService,
                                securityAuditLogger);

                toolRegistry = new AgentToolRegistry(
                                List.of(
                                                mcpTool));

                skillToolScope = new SkillToolScope();

                executionLimits = new AgentExecutionLimits(
                                10,
                                8,
                                2,
                                3,
                                Duration.ofSeconds(120),
                                Duration.ofSeconds(30),
                                20_000,
                                60_000);

                toolActionExecutor = new DefaultToolActionExecutor(
                                toolRegistry,
                                executionLimits,
                                securityAuditLogger);

                AgentSkill legalResearchSkill = AgentSkill.of(
                                "legal-research",
                                "Legal Research",
                                "用于法律问题研究与法律依据检索",
                                "对需要法律依据支持的判断，应优先检索法律知识库",
                                List.of(
                                                LegalKnowledgeTool.TOOL_NAME),
                                Set.of(
                                                "legal",
                                                "research"));

                legalSkillContext = SkillContext.of(
                                legalResearchSkill);
        }

        @Test
        void shouldExposeMcpToolThroughExistingSkillToolScope() {

                List<String> availableTools = skillToolScope.filterAllowed(
                                Optional.of(
                                                legalSkillContext),
                                toolRegistry.names());

                assertEquals(
                                List.of(
                                                LegalKnowledgeTool.TOOL_NAME),
                                availableTools);

                assertTrue(
                                skillToolScope.isAllowed(
                                                Optional.of(
                                                                legalSkillContext),
                                                LegalKnowledgeTool.TOOL_NAME));
        }

        @Test
        void shouldExecuteMcpToolThroughExistingAgentToolPipeline() {

                when(
                                mcpToolCallback.call(
                                                anyString()))
                                .thenReturn(
                                                "《劳动合同法》第八十七条："
                                                                + "用人单位违法解除或者终止劳动合同的，"
                                                                + "应当依照经济补偿标准的二倍向劳动者支付赔偿金。");

                ToolAction action = ToolAction.of(
                                "task-1",
                                LegalKnowledgeTool.TOOL_NAME,
                                Map.of(
                                                LegalKnowledgeTool.LEGAL_QUESTION_ARGUMENT,
                                                "违法解除劳动合同需要承担什么责任"));

                ToolObservation observation = toolActionExecutor.execute(
                                action);

                assertTrue(
                                observation.isSuccess());

                assertEquals(
                                "task-1",
                                observation.getTaskId());

                assertEquals(
                                LegalKnowledgeTool.TOOL_NAME,
                                observation.getToolName());

                assertTrue(
                                observation.getContent()
                                                .contains(
                                                                "第八十七条"));

                assertTrue(
                                observation.getContent()
                                                .contains(
                                                                "二倍"));

                LegalSecurityContext securityContext = observation
                                .getEvidenceSecurityContext()
                                .orElseThrow();

                assertEquals(
                                SecuritySource.MCP_RESULT,
                                securityContext.source());

                assertEquals(
                                SecurityTrustLevel.UNTRUSTED,
                                securityContext.trustLevel());

                verify(
                                mcpToolCallback)
                                .call(
                                                anyString());
        }

        @Test
        void shouldConvertMcpFailureIntoExistingFailedToolObservation() {

                when(
                                mcpToolCallback.call(
                                                anyString()))
                                .thenThrow(
                                                new IllegalStateException(
                                                                "Legal MCP Server unavailable"));

                ToolAction action = ToolAction.of(
                                "task-1",
                                LegalKnowledgeTool.TOOL_NAME,
                                Map.of(
                                                LegalKnowledgeTool.LEGAL_QUESTION_ARGUMENT,
                                                "违法解除劳动合同"));

                ToolObservation observation = toolActionExecutor.execute(
                                action);

                assertTrue(
                                observation.isFailure());

                assertEquals(
                                "task-1",
                                observation.getTaskId());

                assertEquals(
                                LegalKnowledgeTool.TOOL_NAME,
                                observation.getToolName());

                assertEquals(
                                "MCP tool execution failed",
                                observation.getErrorMessage());

                LegalSecurityContext securityContext = observation
                                .getEvidenceSecurityContext()
                                .orElseThrow();

                assertEquals(
                                SecuritySource.MCP_RESULT,
                                securityContext.source());

                assertEquals(
                                SecurityTrustLevel.UNTRUSTED,
                                securityContext.trustLevel());
        }

        @Test
        void shouldKeepMcpImplementationInvisibleToSkillLayer() {

                assertEquals(
                                List.of(
                                                LegalKnowledgeTool.TOOL_NAME),
                                toolRegistry.names());

                assertEquals(
                                "searchLegalKnowledge",
                                legalSkillContext
                                                .getAllowedTools()
                                                .getFirst());

                List<String> availableTools = skillToolScope.filterAllowed(
                                Optional.of(
                                                legalSkillContext),
                                toolRegistry.names());

                assertEquals(
                                List.of(
                                                "searchLegalKnowledge"),
                                availableTools);
        }
}
