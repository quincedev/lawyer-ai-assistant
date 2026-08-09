package com.quince.lawyeraiassistant.prompt.factory;

import com.quince.lawyeraiassistant.prompt.PromptNames;
import com.quince.lawyeraiassistant.prompt.definition.PromptDefinition;
import com.quince.lawyeraiassistant.prompt.model.PromptFragment;
import com.quince.lawyeraiassistant.prompt.registry.DefaultPromptRegistry;
import com.quince.lawyeraiassistant.prompt.registry.PromptRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPromptFactoryTest {

        private PromptRegistry promptRegistry;

        private PromptFactory promptFactory;

        @BeforeEach
        void setUp() {
                promptRegistry = new DefaultPromptRegistry();
                promptFactory = new DefaultPromptFactory(promptRegistry);
        }

        @Test
        void shouldGetPromptByName() {
                PromptFragment fragment = createFragment(
                                "custom-prompt",
                                "这是一个自定义 Prompt。");

                promptRegistry.register(fragment);

                PromptFragment result = promptFactory.get("custom-prompt");

                assertSame(fragment, result);
        }

        @Test
        void shouldGetLawyerSystemPrompt() {
                PromptFragment fragment = createFragment(
                                PromptDefinition.LAWYER_SYSTEM.getName(),
                                "你是一名专业律师。");

                promptRegistry.register(fragment);

                PromptFragment result = promptFactory.lawyerSystem();

                assertSame(fragment, result);
                assertEquals(
                                PromptDefinition.LAWYER_SYSTEM.getName(),
                                result.getName());
                assertEquals(
                                "你是一名专业律师。",
                                result.getContent());
        }

        @Test
        void shouldThrowExceptionWhenPromptDoesNotExist() {
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> promptFactory.get("not-exist"));

                assertTrue(
                                exception.getMessage().contains("not-exist"));
        }

        @Test
        void shouldThrowExceptionWhenLawyerSystemPromptDoesNotExist() {
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> promptFactory.lawyerSystem());

                assertTrue(
                                exception.getMessage()
                                                .contains(PromptDefinition.LAWYER_SYSTEM.getName()));
        }

        @Test
        void shouldThrowExceptionWhenPromptNameIsNull() {
                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> promptFactory.get(null));

                assertEquals(
                                "Prompt name must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenPromptNameIsEmpty() {
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> promptFactory.get(""));

                assertEquals(
                                "Prompt name must not be blank",
                                exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenPromptNameContainsOnlyWhitespace() {
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> promptFactory.get("   "));

                assertEquals(
                                "Prompt name must not be blank",
                                exception.getMessage());
        }

        @Test
        void shouldReturnAgentReflectionPrompt() {

                PromptFragment expected = createFragment(
                                PromptNames.AGENT_REFLECTION,
                                "Agent reflection prompt");

                promptRegistry.register(
                                expected);

                PromptFragment actual = promptFactory.agentReflection();

                assertSame(
                                expected,
                                actual);
        }

        @Test
        void shouldReturnAgentReplanningPrompt() {

                PromptFragment expected = createFragment(
                                PromptNames.AGENT_REPLANNING,
                                "Agent replanning prompt");

                promptRegistry.register(
                                expected);

                PromptFragment actual = promptFactory.agentReplanning();

                assertSame(
                                expected,
                                actual);
        }

        @Test
        void shouldReturnAgentRuntimeReasonPrompt() {

                PromptFragment expected = createFragment(
                                PromptNames.AGENT_RUNTIME_REASON,
                                "Agent runtime reason prompt");

                promptRegistry.register(
                                expected);

                PromptFragment actual = promptFactory.agentRuntimeReason();

                assertSame(
                                expected,
                                actual);
        }

        private PromptFragment createFragment(
                        String name,
                        String content) {
                return PromptFragment.builder()
                                .name(name)
                                .content(content)
                                .version("v1")
                                .source(
                                                "classpath:prompts/system/"
                                                                + name
                                                                + ".txt")
                                .build();
        }
}
