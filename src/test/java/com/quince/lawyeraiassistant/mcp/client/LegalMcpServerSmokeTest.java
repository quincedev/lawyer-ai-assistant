package com.quince.lawyeraiassistant.mcp.client;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Legal MCP Server Smoke Test。
 *
 * <p>
 * 前置条件：
 * </p>
 *
 * <pre>
 * 1. lawyer-ai-assistant 主应用已经运行在 localhost:8080
 * 2. MCP Server protocol = STATELESS
 * 3. /mcp endpoint 可访问
 * </pre>
 *
 * <p>
 * 本测试验证：
 * </p>
 *
 * <pre>
 * MCP Client
 *      ↓
 * tools/list
 *      ↓
 * searchLegalKnowledge
 *      ↓
 * tools/call
 *      ↓
 * Legal MCP Server
 *      ↓
 * RetrievalOrchestrator
 * </pre>
 */
@SpringBootTest
@ActiveProfiles("mcp-client")
@EnabledIfSystemProperty(
        named = "mcp.smoke.enabled",
        matches = "true")
class LegalMcpServerSmokeTest {

    private final List<McpSyncClient> mcpClients;

    @Autowired
    LegalMcpServerSmokeTest(
            List<McpSyncClient> mcpClients) {

        this.mcpClients = mcpClients;
    }

    @Test
    void shouldDiscoverSearchLegalKnowledgeTool() {

        McpSyncClient client = getLegalMcpClient();

        McpSchema.ListToolsResult result = client.listTools();

        assertNotNull(
                result);

        assertNotNull(
                result.tools());

        assertFalse(
                result.tools()
                        .isEmpty());

        boolean found = result.tools()
                .stream()
                .anyMatch(
                        tool -> "searchLegalKnowledge"
                                .equals(
                                        tool.name()));

        assertTrue(
                found,
                "searchLegalKnowledge should be exposed by Legal MCP Server");
    }

    @Test
    void shouldCallSearchLegalKnowledgeTool() {

        McpSyncClient client = getLegalMcpClient();

        McpSchema.CallToolRequest request = McpSchema.CallToolRequest
                .builder("searchLegalKnowledge")
                .arguments(
                        Map.of(
                                "legalQuestion",
                                "违法解除劳动合同的赔偿标准是什么"))
                .build();

        McpSchema.CallToolResult result = client.callTool(
                request);

        assertNotNull(
                result);

        assertFalse(
                Boolean.TRUE.equals(
                        result.isError()));

        assertNotNull(
                result.content());

        assertFalse(
                result.content()
                        .isEmpty());

        String combinedContent = result.content()
                .stream()
                .map(
                        Object::toString)
                .reduce(
                        "",
                        (left, right) -> left
                                + System.lineSeparator()
                                + right);

        assertTrue(
                combinedContent.contains(
                        "劳动合同"));

        assertTrue(
                combinedContent.contains(
                        "赔偿"));
    }

    private McpSyncClient getLegalMcpClient() {

        assertNotNull(
                mcpClients);

        assertFalse(
                mcpClients.isEmpty(),
                "No McpSyncClient bean found");

        /*
         * 当前 Smoke Test 只配置一个 MCP Server，
         * 所以直接取第一个 Client。
         *
         * 后续如果同时连接多个 MCP Server，
         * 再增加按 Server Name / Connection Name 的选择逻辑。
         */
        return mcpClients.getFirst();
    }
}
