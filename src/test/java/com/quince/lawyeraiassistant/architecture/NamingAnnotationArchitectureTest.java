package com.quince.lawyeraiassistant.architecture;

import com.quince.lawyeraiassistant.security.identity.ApplicationUserRepository;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import org.apache.ibatis.annotations.Mapper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class NamingAnnotationArchitectureTest {

    private static final String BASE_PACKAGE = "com.quince.lawyeraiassistant";

    private static final String MYBATIS_PACKAGE = BASE_PACKAGE
            + ".security.identity.persistence.mybatis..";

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
     * Controller
     * =========================================================
     */

    @Test
    void controllersShouldUseRestControllerAnnotation() {

        ArchRule rule = classes()
                .that()
                .haveSimpleNameEndingWith(
                        "Controller")
                .should()
                .beAnnotatedWith(
                        RestController.class)
                .because(
                        "HTTP endpoint classes must use the "
                                + "@RestController stereotype");

        rule.check(
                applicationClasses);
    }

    @Test
    void restControllersShouldUseControllerSuffix() {

        ArchRule rule = classes()
                .that()
                .areAnnotatedWith(
                        RestController.class)
                .should()
                .haveSimpleNameEndingWith(
                        "Controller")
                .because(
                        "REST endpoint classes must be identifiable "
                                + "by the Controller suffix");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Configuration
     * =========================================================
     */

    @Test
    void configurationClassesShouldUseConfigurationAnnotation() {

        ArchRule rule = classes()
                .that()
                .haveSimpleNameEndingWith(
                        "Configuration")
                .should()
                .beAnnotatedWith(
                        Configuration.class)
                .because(
                        "Spring configuration classes must explicitly "
                                + "declare @Configuration");

        rule.check(
                applicationClasses);
    }

    @Test
    void configurationAnnotatedClassesShouldUseConfigurationSuffix() {

        ArchRule rule = classes()
                .that()
                .areAnnotatedWith(
                        Configuration.class)
                .should()
                .haveSimpleNameEndingWith(
                        "Configuration")
                .because(
                        "Spring configuration classes must be "
                                + "recognizable by name");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Configuration Properties
     * =========================================================
     */

    @Test
    void propertiesClassesShouldUseConfigurationPropertiesAnnotation() {

        ArchRule rule = classes()
                .that()
                .haveSimpleNameEndingWith(
                        "Properties")
                .should()
                .beAnnotatedWith(
                        ConfigurationProperties.class)
                .because(
                        "typed application properties must be bound "
                                + "through @ConfigurationProperties");

        rule.check(
                applicationClasses);
    }

    @Test
    void configurationPropertiesShouldUsePropertiesSuffix() {

        ArchRule rule = classes()
                .that()
                .areAnnotatedWith(
                        ConfigurationProperties.class)
                .should()
                .haveSimpleNameEndingWith(
                        "Properties")
                .because(
                        "configuration property types must be "
                                + "recognizable by name");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * MyBatis Mapper
     * =========================================================
     */

    @Test
    void myBatisMappersShouldBeAnnotatedInterfaces() {

        ArchRule mapperAnnotationRule = classes()
                .that()
                .resideInAPackage(
                        MYBATIS_PACKAGE)
                .and()
                .haveSimpleNameEndingWith(
                        "Mapper")
                .should()
                .beAnnotatedWith(
                        Mapper.class)
                .because(
                        "MyBatis mapper contracts must declare @Mapper");

        ArchRule mapperInterfaceRule = classes()
                .that()
                .resideInAPackage(
                        MYBATIS_PACKAGE)
                .and()
                .haveSimpleNameEndingWith(
                        "Mapper")
                .should()
                .beInterfaces()
                .because(
                        "MyBatis mapper types must remain interfaces");

        mapperAnnotationRule.check(
                applicationClasses);

        mapperInterfaceRule.check(
                applicationClasses);
    }

    @Test
    void mapperAnnotatedTypesShouldRemainInPersistenceAdapter() {

        ArchRule packageRule = classes()
                .that()
                .areAnnotatedWith(
                        Mapper.class)
                .should()
                .resideInAPackage(
                        MYBATIS_PACKAGE)
                .because(
                        "MyBatis infrastructure must remain inside "
                                + "the persistence adapter");

        ArchRule namingRule = classes()
                .that()
                .areAnnotatedWith(
                        Mapper.class)
                .should()
                .haveSimpleNameEndingWith(
                        "Mapper")
                .because(
                        "MyBatis mapper types must use the Mapper suffix");

        packageRule.check(
                applicationClasses);

        namingRule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Exception Naming
     * =========================================================
     */

    @Test
    void applicationExceptionsShouldUseExceptionSuffix() {

        ArchRule rule = classes()
                .that()
                .areAssignableTo(
                        Throwable.class)
                .should()
                .haveSimpleNameEndingWith(
                        "Exception")
                .because(
                        "application exception types must be "
                                + "immediately identifiable");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Repository Naming
     * =========================================================
     */

    @Test
    void repositoryAdaptersShouldUseRepositorySuffix() {

        ArchRule rule = classes()
                .that()
                .implement(
                        ApplicationUserRepository.class)
                .should()
                .haveSimpleNameEndingWith(
                        "Repository")
                .because(
                        "identity repository adapters must be "
                                + "recognizable by name");

        rule.check(
                applicationClasses);
    }

    /*
     * =========================================================
     * Dependency Injection Style
     * =========================================================
     */

    @Test
    void applicationShouldNotUseFieldInjection() {

        NO_CLASSES_SHOULD_USE_FIELD_INJECTION
                .because(
                        "constructor injection makes dependencies explicit "
                                + "and keeps classes independently testable")
                .check(
                        applicationClasses);
    }
}