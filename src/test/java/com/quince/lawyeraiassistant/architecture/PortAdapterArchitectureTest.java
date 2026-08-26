package com.quince.lawyeraiassistant.architecture;

import com.quince.lawyeraiassistant.agent.application.AgentApplicationService;
import com.quince.lawyeraiassistant.agent.runtime.AgentRuntime;
import com.quince.lawyeraiassistant.agent.tool.legal.mcp.LegalKnowledgeMcpClient;
import com.quince.lawyeraiassistant.agent.tool.legal.mcp.result.McpLegalResultProcessor;
import com.quince.lawyeraiassistant.cache.retrieval.RetrievalCache;
import com.quince.lawyeraiassistant.cache.tool.ToolResultCache;
import com.quince.lawyeraiassistant.retrieval.parent.provider.ParentDocumentProvider;
import com.quince.lawyeraiassistant.retrieval.parent.provider.ParentDocumentStore;
import com.quince.lawyeraiassistant.security.identity.ApplicationUserRepository;
import com.quince.lawyeraiassistant.workflow.executor.WorkflowExecutor;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PortAdapterArchitectureTest {

    private static final String BASE_PACKAGE = "com.quince.lawyeraiassistant";

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
     * Cache Port / Adapter
     * =========================================================
     */

    @Test
    void cacheAdaptersShouldImplementCachePorts() {

        requireImplementation(
                "CaffeineRetrievalCache",
                RetrievalCache.class);

        requireImplementation(
                "RedisRetrievalCache",
                RetrievalCache.class);

        requireImplementation(
                "CaffeineToolResultCache",
                ToolResultCache.class);

        requireImplementation(
                "RedisToolResultCache",
                ToolResultCache.class);
    }

    @Test
    void cacheConsumersShouldNotDependOnConcreteAdapters() {

        forbidConcreteDependencyOutsidePackages(
                BASE_PACKAGE
                        + ".cache.retrieval.CaffeineRetrievalCache",
                BASE_PACKAGE + ".cache.retrieval..",
                BASE_PACKAGE + ".cache.config..");

        forbidConcreteDependencyOutsidePackages(
                BASE_PACKAGE
                        + ".cache.redis.RedisRetrievalCache",
                BASE_PACKAGE + ".cache.redis..",
                BASE_PACKAGE + ".cache.config..");

        forbidConcreteDependencyOutsidePackages(
                BASE_PACKAGE
                        + ".cache.tool.CaffeineToolResultCache",
                BASE_PACKAGE + ".cache.tool..",
                BASE_PACKAGE + ".cache.config..");

        forbidConcreteDependencyOutsidePackages(
                BASE_PACKAGE
                        + ".cache.redis.RedisToolResultCache",
                BASE_PACKAGE + ".cache.redis..",
                BASE_PACKAGE + ".cache.config..");
    }

    /*
     * =========================================================
     * Identity Port / Adapter
     * =========================================================
     */

    @Test
    void identityAdaptersShouldImplementRepositoryPort() {

        requireImplementation(
                "InMemoryApplicationUserRepository",
                ApplicationUserRepository.class);

        requireImplementation(
                "MyBatisApplicationUserRepository",
                ApplicationUserRepository.class);
    }

    @Test
    void identityConsumersShouldNotDependOnRepositoryImplementations() {

        forbidConcreteDependencyOutsidePackages(
                BASE_PACKAGE
                        + ".security.identity.inmemory."
                        + "InMemoryApplicationUserRepository",
                BASE_PACKAGE
                        + ".security.identity.inmemory..",
                BASE_PACKAGE
                        + ".security.authentication.config..");

        forbidConcreteDependencyOutsidePackages(
                BASE_PACKAGE
                        + ".security.identity.persistence.mybatis."
                        + "MyBatisApplicationUserRepository",
                BASE_PACKAGE
                        + ".security.identity.persistence.mybatis..",
                BASE_PACKAGE
                        + ".security.authentication.config..");
    }

    /*
     * =========================================================
     * MCP Client Port / Adapter
     * =========================================================
     */

    @Test
    void mcpAdaptersShouldImplementMcpPorts() {

        requireImplementation(
                "SpringAiLegalKnowledgeMcpClient",
                LegalKnowledgeMcpClient.class);

        requireImplementation(
                "DefaultMcpLegalResultProcessor",
                McpLegalResultProcessor.class);
    }

    @Test
    void mcpToolShouldNotDependOnConcreteMcpAdapters() {

        forbidConcreteDependencyOutsidePackages(
                BASE_PACKAGE
                        + ".agent.tool.legal.mcp."
                        + "SpringAiLegalKnowledgeMcpClient",
                BASE_PACKAGE
                        + ".agent.tool.legal.mcp..");

        forbidConcreteDependencyOutsidePackages(
                BASE_PACKAGE
                        + ".agent.tool.legal.mcp.result."
                        + "DefaultMcpLegalResultProcessor",
                BASE_PACKAGE
                        + ".agent.tool.legal.mcp.result..");
    }

    /*
     * =========================================================
     * Agent Application / Runtime
     * =========================================================
     */

    @Test
    void agentImplementationsShouldImplementApplicationPorts() {

        requireImplementation(
                "DefaultAgentRuntime",
                AgentRuntime.class);

        requireImplementation(
                "DefaultAgentApplicationService",
                AgentApplicationService.class);
    }

    @Test
    void agentControllersShouldNotDependOnDefaultImplementations() {

        forbidConcreteDependencyOutsidePackages(
                BASE_PACKAGE
                        + ".agent.runtime.DefaultAgentRuntime",
                BASE_PACKAGE + ".agent.runtime..",
                BASE_PACKAGE + ".agent.config..");

        forbidConcreteDependencyOutsidePackages(
                BASE_PACKAGE
                        + ".agent.application."
                        + "DefaultAgentApplicationService",
                BASE_PACKAGE + ".agent.application..",
                BASE_PACKAGE + ".agent.config..");
    }

    /*
     * =========================================================
     * Workflow
     * =========================================================
     */

    @Test
    void workflowImplementationShouldImplementExecutorPort() {

        requireImplementation(
                "DefaultWorkflowExecutor",
                WorkflowExecutor.class);
    }

    @Test
    void workflowConsumersShouldNotDependOnDefaultExecutor() {

        forbidConcreteDependencyOutsidePackages(
                BASE_PACKAGE
                        + ".workflow.executor.DefaultWorkflowExecutor",
                BASE_PACKAGE + ".workflow.executor..",
                BASE_PACKAGE + ".workflow.config..");
    }

    /*
     * =========================================================
     * Parent Document Port
     * =========================================================
     */

    @Test
    void parentDocumentAdapterShouldImplementBothPorts() {

        requireImplementation(
                "InMemoryParentDocumentProvider",
                ParentDocumentProvider.class);

        requireImplementation(
                "InMemoryParentDocumentProvider",
                ParentDocumentStore.class);
    }

    /*
     * =========================================================
     * Shared Helpers
     * =========================================================
     */

    private static void requireImplementation(
            String implementationSimpleName,
            Class<?> portType) {

        ArchRule rule = classes()
                .that()
                .haveSimpleName(
                        implementationSimpleName)
                .should()
                .implement(
                        portType)
                .because(
                        implementationSimpleName
                                + " is an adapter and must implement the "
                                + portType.getSimpleName()
                                + " port");

        rule.check(
                applicationClasses);
    }

    private static void forbidConcreteDependencyOutsidePackages(
            String implementationClassName,
            String... allowedPackages) {

        ArchRule rule = noClasses()
                .that()
                .resideOutsideOfPackages(
                        allowedPackages)
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        implementationClassName)
                .because(
                        "application consumers must depend on ports, "
                                + "not concrete adapter "
                                + implementationClassName);

        rule.check(
                applicationClasses);
    }
}