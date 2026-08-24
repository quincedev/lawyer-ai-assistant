package com.quince.lawyeraiassistant.security.authentication.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.quince.lawyeraiassistant.security.identity.ApplicationUserRepository;
import com.quince.lawyeraiassistant.security.identity.inmemory.InMemoryApplicationUserRepository;
import com.quince.lawyeraiassistant.security.identity.persistence.mybatis.ApplicationUserMapper;
import com.quince.lawyeraiassistant.security.identity.persistence.mybatis.MyBatisApplicationUserRepository;

class IdentityRepositoryConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    DevelopmentIdentityConfiguration.class,
                    MyBatisIdentityConfiguration.class)
            .withBean(
                    PasswordEncoder.class,
                    PasswordEncoderFactories::createDelegatingPasswordEncoder)
            .withBean(
                    ApplicationUserMapper.class,
                    () -> mock(
                            ApplicationUserMapper.class));

    @Test
    void shouldUseInMemoryRepositoryByDefault() {

        contextRunner.run(
                context -> {

                    assertThat(context)
                            .hasSingleBean(
                                    ApplicationUserRepository.class);

                    assertThat(context.getBean(
                            ApplicationUserRepository.class))
                            .isInstanceOf(
                                    InMemoryApplicationUserRepository.class);
                });
    }

    @Test
    void shouldUseExplicitInMemoryRepository() {

        contextRunner
                .withPropertyValues(
                        "app.security.identity.store=in-memory")
                .run(
                        context -> {

                            assertThat(context)
                                    .hasSingleBean(
                                            ApplicationUserRepository.class);

                            assertThat(context.getBean(
                                    ApplicationUserRepository.class))
                                    .isInstanceOf(
                                            InMemoryApplicationUserRepository.class);
                        });
    }

    @Test
    void shouldUseMyBatisRepositoryWhenConfigured() {

        contextRunner
                .withPropertyValues(
                        "app.security.identity.store=mybatis")
                .run(
                        context -> {

                            assertThat(context)
                                    .hasSingleBean(
                                            ApplicationUserRepository.class);

                            assertThat(context.getBean(
                                    ApplicationUserRepository.class))
                                    .isInstanceOf(
                                            MyBatisApplicationUserRepository.class);
                        });
    }

    @Test
    void shouldNeverRegisterBothRepositoryImplementations() {

        contextRunner
                .withPropertyValues(
                        "app.security.identity.store=mybatis")
                .run(
                        context -> {

                            assertThat(context)
                                    .hasSingleBean(
                                            ApplicationUserRepository.class);

                            assertThat(context)
                                    .doesNotHaveBean(
                                            InMemoryApplicationUserRepository.class);
                        });
    }
}