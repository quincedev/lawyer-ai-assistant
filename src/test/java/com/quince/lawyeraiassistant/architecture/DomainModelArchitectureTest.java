package com.quince.lawyeraiassistant.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainModelArchitectureTest {

    private static final String BASE_PACKAGE = "com.quince.lawyeraiassistant";

    private static final String AGENT_MODEL = BASE_PACKAGE + ".agent.model..";

    private static final String WORKFLOW_MODEL = BASE_PACKAGE + ".workflow.model..";

    private static final String QUERY_MODEL = BASE_PACKAGE + ".query.model..";

    private static final String KNOWLEDGE_CONTRACT = BASE_PACKAGE + ".knowledge.contract";

    /*
     * 这里只匹配 security.identity 根包，
     * 不包含 persistence 和 inmemory 子包。
     */
    private static final String IDENTITY_DOMAIN = BASE_PACKAGE + ".security.identity";

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
     * Framework Independence
     * =========================================================
     */

    @Test
    void coreModelsShouldNotDependOnRuntimeFrameworks() {

        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage(
                        AGENT_MODEL,
                        WORKFLOW_MODEL,
                        QUERY_MODEL)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        /*
                         * Spring Framework / Spring Boot
                         */
                        "org.springframework..",

                        /*
                         * Spring AI
                         */
                        "org.springframework.ai..",

                        /*
                         * Persistence
                         */
                        "org.apache.ibatis..",
                        "org.mybatis..",
                        "jakarta.persistence..",

                        /*
                         * Cache infrastructure
                         */
                        "org.springframework.data.redis..",
                        "io.lettuce..",
                        "com.github.benmanes.caffeine..",

                        /*
                         * MCP infrastructure
                         */
                        "io.modelcontextprotocol..",

                        /*
                         * Web infrastructure
                         */
                        "jakarta.servlet..",

                        /*
                         * JSON serialization boundary
                         */
                        "tools.jackson..",
                        "com.fasterxml.jackson..")
                .because(
                        "core domain models must describe business state "
                                + "without depending on runtime frameworks");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Adapter Independence
     * =========================================================
     */

    @Test
    void coreModelsShouldNotDependOnAdapters() {

        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage(
                        AGENT_MODEL,
                        WORKFLOW_MODEL,
                        QUERY_MODEL)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        BASE_PACKAGE + ".cache.redis..",
                        BASE_PACKAGE
                                + ".security.identity.persistence..",
                        BASE_PACKAGE
                                + ".security.identity.inmemory..",
                        BASE_PACKAGE + ".mcp.server..",
                        BASE_PACKAGE + ".controller..",
                        BASE_PACKAGE + ".web..")
                .because(
                        "domain models must not know about Redis, MyBatis, "
                                + "MCP Server or web adapter implementations");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Presentation Independence
     * =========================================================
     */

    @Test
    void coreModelsShouldNotDependOnPresentationDtos() {

        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage(
                        AGENT_MODEL,
                        WORKFLOW_MODEL,
                        QUERY_MODEL)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        BASE_PACKAGE + ".dto..",
                        BASE_PACKAGE + ".agent.dto..",
                        BASE_PACKAGE + ".rag.dto..")
                .because(
                        "domain state must be mapped to presentation DTOs, "
                                + "not depend on them");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Knowledge Contract
     * =========================================================
     */

    @Test
    void knowledgeContractsShouldOnlyDependOnJavaAndThemselves() {

        ArchRule rule = classes()
                .that()
                .resideInAPackage(
                        KNOWLEDGE_CONTRACT)
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "java..",
                        KNOWLEDGE_CONTRACT)
                .because(
                        "shared knowledge contracts must remain independent "
                                + "from Agent, MCP and Retrieval implementations");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Identity Domain / Repository Port
     * =========================================================
     */

    @Test
    void identityDomainShouldOnlyDependOnJavaAndItself() {

        ArchRule rule = classes()
                .that()
                .resideInAPackage(
                        IDENTITY_DOMAIN)
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "java..",
                        IDENTITY_DOMAIN)
                .because(
                        "identity entities and repository ports must remain "
                                + "independent from MyBatis and Spring Security");

        rule.check(
                applicationClasses);
    }
}