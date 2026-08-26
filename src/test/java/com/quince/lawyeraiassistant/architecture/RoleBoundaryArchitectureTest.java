package com.quince.lawyeraiassistant.architecture;

import com.quince.lawyeraiassistant.agent.controller.AgentPlaygroundController;
import com.quince.lawyeraiassistant.agent.tool.legal.LegalKnowledgeTool;
import com.quince.lawyeraiassistant.agent.tool.legal.McpLegalKnowledgeTool;
import com.quince.lawyeraiassistant.agent.tool.legal.mcp.SpringAiLegalKnowledgeMcpClient;
import com.quince.lawyeraiassistant.controller.RetrievalPlaygroundController;
import com.quince.lawyeraiassistant.mcp.server.legal.LegalMcpTools;
import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.rag.vector.initializer.KnowledgeBaseInitializer;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import static org.assertj.core.api.Assertions.assertThat;

class RoleBoundaryArchitectureTest {

    private static final String BASE_PACKAGE = "com.quince.lawyeraiassistant";

    private static final String AGENT_PACKAGE = BASE_PACKAGE + ".agent..";

    private static final String RAG_PACKAGE = BASE_PACKAGE + ".rag..";

    private static final String RETRIEVAL_PACKAGE = BASE_PACKAGE + ".retrieval..";

    private static final String MCP_SERVER_PACKAGE = BASE_PACKAGE + ".mcp.server..";

    private static final String AGENT_MCP_PACKAGE = BASE_PACKAGE + ".agent.tool.legal.mcp..";

    private static JavaClasses applicationClasses;

    @BeforeAll
    static void importApplicationClasses() {

        applicationClasses = new ClassFileImporter()
                .withImportOption(
                        new ImportOption.DoNotIncludeTests())
                .importPackages(
                        BASE_PACKAGE);
    }

    /*
     * =========================================================
     * RAG / Retrieval Must Not Depend On Agent
     * =========================================================
     */

    @Test
    void ragAndRetrievalShouldNotDependOnAgentRuntime() {

        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage(
                        RAG_PACKAGE,
                        RETRIEVAL_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        AGENT_PACKAGE)
                .because(
                        "RAG and Retrieval are reusable capabilities "
                                + "and must not depend on Agent Runtime");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * MCP Server Must Not Depend On Agent
     * =========================================================
     */

    @Test
    void mcpServerShouldNotDependOnAgentImplementation() {

        ArchRule rule = noClasses()
                .that()
                .resideInAPackage(
                        MCP_SERVER_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        AGENT_PACKAGE)
                .because(
                        "MCP Server exposes Retrieval through the protocol "
                                + "and must not depend on Agent implementation");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Agent MCP Path Must Not Bypass MCP Server
     * =========================================================
     */

    @Test
    void agentMcpClientPackageShouldNotDependOnLocalRag() {

        ArchRule rule = noClasses()
                .that()
                .resideInAPackage(
                        AGENT_MCP_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        RAG_PACKAGE,
                        RETRIEVAL_PACKAGE,
                        MCP_SERVER_PACKAGE)
                .because(
                        "the Agent MCP client path must call the MCP protocol "
                                + "instead of directly calling local RAG");

        rule.check(
                applicationClasses);
    }

    @Test
    void mcpLegalToolShouldNotDependOnLocalRag() {

        ArchRule rule = noClasses()
                .that()
                .haveSimpleName(
                        "McpLegalKnowledgeTool")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        RAG_PACKAGE,
                        RETRIEVAL_PACKAGE,
                        MCP_SERVER_PACKAGE)
                .because(
                        "McpLegalKnowledgeTool must use "
                                + "LegalKnowledgeMcpClient and not local RAG");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Role Adapters Must Not Directly Access Embedding Storage
     * =========================================================
     */

    @Test
    void roleAdaptersShouldNotDirectlyUseEmbeddingInfrastructure() {

        ArchRule rule = noClasses()
                .that()
                .haveSimpleNameEndingWith(
                        "McpTools")
                .or()
                .haveSimpleName(
                        "McpLegalKnowledgeTool")
                .or()
                .haveSimpleName(
                        "SpringAiLegalKnowledgeMcpClient")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.ai.ollama..",
                        "org.springframework.ai.embedding..",
                        "org.springframework.ai.vectorstore..")
                .because(
                        "role adapters must delegate embedding and vector "
                                + "search to the RAG/Retrieval layer");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Required Role Annotations
     * =========================================================
     */

    @Test
    void mcpServerToolShouldHaveProfileAndRagConditions() {

        classes()
                .that()
                .haveSimpleName(
                        "LegalMcpTools")
                .should()
                .beAnnotatedWith(
                        Profile.class)
                .andShould()
                .beAnnotatedWith(
                        ConditionalOnRagEnabled.class)
                .because(
                        "LegalMcpTools belongs only to the MCP Server "
                                + "and requires local RAG")
                .check(
                        applicationClasses);

        assertProfile(
                LegalMcpTools.class,
                "mcp-server");
    }

    @Test
    void localAndMcpLegalToolsShouldUseExplicitBackendConditions() {

        classes()
                .that()
                .haveSimpleName(
                        "LegalKnowledgeTool")
                .should()
                .beAnnotatedWith(
                        ConditionalOnRagEnabled.class)
                .andShould()
                .beAnnotatedWith(
                        ConditionalOnProperty.class)
                .because(
                        "the local legal tool requires both local mode "
                                + "and enabled RAG")
                .check(
                        applicationClasses);

        classes()
                .that()
                .haveSimpleName(
                        "McpLegalKnowledgeTool")
                .should()
                .beAnnotatedWith(
                        ConditionalOnProperty.class)
                .because(
                        "the MCP legal tool must only exist in MCP mode")
                .check(
                        applicationClasses);

        classes()
                .that()
                .haveSimpleName(
                        "SpringAiLegalKnowledgeMcpClient")
                .should()
                .beAnnotatedWith(
                        ConditionalOnProperty.class)
                .because(
                        "the concrete MCP client must only exist in MCP mode")
                .check(
                        applicationClasses);

        assertConditionalProperty(
                LegalKnowledgeTool.class,
                "app.agent",
                "legal-tool-mode",
                "local",
                true);

        assertConditionalProperty(
                McpLegalKnowledgeTool.class,
                "app.agent",
                "legal-tool-mode",
                "mcp",
                false);

        assertConditionalProperty(
                SpringAiLegalKnowledgeMcpClient.class,
                "app.agent",
                "legal-tool-mode",
                "mcp",
                false);
    }

    @Test
    void roleControllersAndInitializerShouldHaveRoleConditions() {

        classes()
                .that()
                .haveSimpleName(
                        "RetrievalPlaygroundController")
                .should()
                .beAnnotatedWith(
                        ConditionalOnRagEnabled.class)
                .because(
                        "the Retrieval playground must not be registered "
                                + "in the Agent role")
                .check(
                        applicationClasses);

        classes()
                .that()
                .haveSimpleName(
                        "KnowledgeBaseInitializer")
                .should()
                .beAnnotatedWith(
                        ConditionalOnRagEnabled.class)
                .because(
                        "knowledge initialization must only run "
                                + "when local RAG is enabled")
                .check(
                        applicationClasses);

        classes()
                .that()
                .haveSimpleName(
                        "AgentPlaygroundController")
                .should()
                .beAnnotatedWith(
                        Profile.class)
                .because(
                        "Agent API must not be exposed by MCP Server")
                .check(
                        applicationClasses);

        assertProfile(
                AgentPlaygroundController.class,
                "!mcp-server");

        assertThat(
                RetrievalPlaygroundController.class
                        .isAnnotationPresent(
                                ConditionalOnRagEnabled.class))
                .isTrue();

        assertThat(
                KnowledgeBaseInitializer.class
                        .isAnnotationPresent(
                                ConditionalOnRagEnabled.class))
                .isTrue();
    }

    /*
     * =========================================================
     * Annotation Semantic Helpers
     * =========================================================
     */

    private static void assertProfile(
            Class<?> componentType,
            String expectedProfile) {

        Profile profile = componentType.getAnnotation(
                Profile.class);

        assertThat(profile)
                .as(
                        componentType.getSimpleName()
                                + " must declare @Profile")
                .isNotNull();

        assertThat(profile.value())
                .containsExactly(
                        expectedProfile);
    }

    private static void assertConditionalProperty(
            Class<?> componentType,
            String expectedPrefix,
            String expectedName,
            String expectedValue,
            boolean expectedMatchIfMissing) {

        ConditionalOnProperty condition = componentType.getAnnotation(
                ConditionalOnProperty.class);

        assertThat(condition)
                .as(
                        componentType.getSimpleName()
                                + " must declare @ConditionalOnProperty")
                .isNotNull();

        assertThat(condition.prefix())
                .isEqualTo(
                        expectedPrefix);

        assertThat(condition.name())
                .containsExactly(
                        expectedName);

        assertThat(condition.havingValue())
                .isEqualTo(
                        expectedValue);

        assertThat(condition.matchIfMissing())
                .isEqualTo(
                        expectedMatchIfMissing);
    }
}