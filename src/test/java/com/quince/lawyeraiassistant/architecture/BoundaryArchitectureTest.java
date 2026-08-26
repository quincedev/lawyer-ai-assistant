package com.quince.lawyeraiassistant.architecture;

import com.quince.lawyeraiassistant.cache.retrieval.RetrievalCache;
import com.quince.lawyeraiassistant.cache.tool.ToolResultCache;
import com.quince.lawyeraiassistant.security.identity.ApplicationUserRepository;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class BoundaryArchitectureTest {

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
     * Domain Model Independence
     * =========================================================
     */

    @Test
    void domainModelsShouldNotDependOnFrameworks() {

        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage(
                        BASE_PACKAGE + ".agent.model..",
                        BASE_PACKAGE + ".workflow.model..",
                        BASE_PACKAGE + ".query.model..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "org.apache.ibatis..",
                        "org.mybatis..",
                        "org.springframework.data..",
                        "com.github.benmanes.caffeine..",
                        "jakarta.persistence..")
                .because(
                        "Agent, Workflow and Query models "
                                + "must remain framework-independent");

        rule.check(
                applicationClasses);
    }

    @Test
    void identityDomainShouldNotDependOnPersistenceAdapters() {

        ArchRule rule = noClasses()
                .that()
                .resideInAPackage(
                        BASE_PACKAGE + ".security.identity")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        BASE_PACKAGE
                                + ".security.identity.persistence..",
                        BASE_PACKAGE
                                + ".security.identity.inmemory..")
                .because(
                        "the identity domain and repository port "
                                + "must not depend on adapter implementations");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Identity Port / Adapter
     * =========================================================
     */

    @Test
    void myBatisIdentityAdapterShouldImplementRepositoryPort() {

        classes()
                .that()
                .haveSimpleName(
                        "MyBatisApplicationUserRepository")
                .should()
                .implement(
                        ApplicationUserRepository.class)
                .because(
                        "the MyBatis identity adapter must implement "
                                + "the application repository port")
                .check(
                        applicationClasses);
    }

    @Test
    void myBatisImplementationDetailsShouldRemainInsideAdapterBoundary() {

        ArchRule rule = noClasses()
                .that()
                .resideOutsideOfPackages(
                        BASE_PACKAGE
                                + ".security.identity.persistence.mybatis..",
                        BASE_PACKAGE
                                + ".security.authentication.config..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        BASE_PACKAGE
                                + ".security.identity.persistence.mybatis..")
                .because(
                        "MyBatis rows, mappers and adapter classes "
                                + "must not leak into application code");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Redis Cache Port / Adapter
     * =========================================================
     */

    @Test
    void redisRetrievalAdapterShouldImplementRetrievalCachePort() {

        classes()
                .that()
                .haveSimpleName(
                        "RedisRetrievalCache")
                .should()
                .implement(
                        RetrievalCache.class)
                .because(
                        "the Redis retrieval adapter must implement "
                                + "the RetrievalCache port")
                .check(
                        applicationClasses);
    }

    @Test
    void redisToolAdapterShouldImplementToolResultCachePort() {

        classes()
                .that()
                .haveSimpleName(
                        "RedisToolResultCache")
                .should()
                .implement(
                        ToolResultCache.class)
                .because(
                        "the Redis tool adapter must implement "
                                + "the ToolResultCache port")
                .check(
                        applicationClasses);
    }

    @Test
    void redisImplementationDetailsShouldRemainInsideCacheAdapter() {

        ArchRule rule = noClasses()
                .that()
                .resideOutsideOfPackages(
                        BASE_PACKAGE + ".cache.redis..",
                        BASE_PACKAGE + ".cache.config..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        BASE_PACKAGE + ".cache.redis..")
                .because(
                        "Redis serializers, key mappers and namespace "
                                + "cleaners must not leak into consumers");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * MCP Server Adapter
     * =========================================================
     */

    @Test
    void mcpServerImplementationShouldNotBeUsedByOtherModules() {

        ArchRule rule = noClasses()
                .that()
                .resideOutsideOfPackage(
                        BASE_PACKAGE + ".mcp.server..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(
                        BASE_PACKAGE + ".mcp.server..")
                .because(
                        "MCP Server adapters must be invoked through "
                                + "the MCP protocol, not through Java imports");

        rule.check(
                applicationClasses);
    }
}