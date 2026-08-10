package com.quince.lawyeraiassistant.agent.tool;

import com.quince.lawyeraiassistant.agent.model.ToolAction;
import com.quince.lawyeraiassistant.agent.model.ToolExecutionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolRegistryTest {

        @Test
        void shouldRegisterAndGetTool() {

                AgentTool tool = createTool(
                                "searchLegalKnowledge");

                AgentToolRegistry registry = new AgentToolRegistry(
                                List.of(tool));

                assertEquals(
                                1,
                                registry.size());

                assertTrue(
                                registry.contains(
                                                "searchLegalKnowledge"));

                assertSame(
                                tool,
                                registry.get(
                                                "searchLegalKnowledge"));
        }

        @Test
        void shouldTrimToolNameWhenLookingUp() {

                AgentTool tool = createTool(
                                "searchLegalKnowledge");

                AgentToolRegistry registry = new AgentToolRegistry(
                                List.of(tool));

                assertSame(
                                tool,
                                registry.get(
                                                "  searchLegalKnowledge  "));
        }

        @Test
        void shouldReturnFalseForUnknownTool() {

                AgentToolRegistry registry = new AgentToolRegistry(
                                List.of(
                                                createTool(
                                                                "searchLegalKnowledge")));

                assertFalse(
                                registry.contains(
                                                "unknownTool"));
        }

        @Test
        void shouldReturnFalseForNullOrBlankToolName() {

                AgentToolRegistry registry = new AgentToolRegistry(
                                List.of());

                assertFalse(
                                registry.contains(null));

                assertFalse(
                                registry.contains("   "));
        }

        @Test
        void shouldRejectUnknownTool() {

                AgentToolRegistry registry = new AgentToolRegistry(
                                List.of());

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> registry.get(
                                                "unknownTool"));

                assertEquals(
                                "Agent tool not found: unknownTool",
                                exception.getMessage());
        }

        @Test
        void shouldRejectDuplicateToolNames() {

                AgentTool firstTool = createTool(
                                "searchLegalKnowledge");

                AgentTool secondTool = createTool(
                                "searchLegalKnowledge");

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> new AgentToolRegistry(
                                                List.of(
                                                                firstTool,
                                                                secondTool)));

                assertEquals(
                                "Duplicate Agent tool name: searchLegalKnowledge",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullToolList() {

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new AgentToolRegistry(
                                                null));

                assertEquals(
                                "Agent tools must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullToolElement() {

                List<AgentTool> tools = new java.util.ArrayList<>();

                tools.add(null);

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new AgentToolRegistry(
                                                tools));

                assertEquals(
                                "Agent tool must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectNullRegisteredToolName() {

                AgentTool tool = createTool(null);

                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new AgentToolRegistry(
                                                List.of(tool)));

                assertEquals(
                                "Agent tool name must not be null",
                                exception.getMessage());
        }

        @Test
        void shouldRejectBlankRegisteredToolName() {

                AgentTool tool = createTool("   ");

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> new AgentToolRegistry(
                                                List.of(tool)));

                assertEquals(
                                "Agent tool name must not be blank",
                                exception.getMessage());
        }

        @Test
        void shouldReturnRegisteredToolNames() {

                AgentTool firstTool = createTool(
                                "searchLegalKnowledge");

                AgentTool secondTool = createTool(
                                "queryCustomer");

                AgentToolRegistry registry = new AgentToolRegistry(
                                List.of(
                                                firstTool,
                                                secondTool));

                assertEquals(
                                2,
                                registry.names()
                                                .size());

                assertTrue(
                                registry.names()
                                                .contains(
                                                                "searchLegalKnowledge"));

                assertTrue(
                                registry.names()
                                                .contains(
                                                                "queryCustomer"));
        }

        private AgentTool createTool(
                        String name) {

                return new AgentTool() {

                        @Override
                        public String name() {
                                return name;
                        }

                        @Override
                        public ToolExecutionResult execute(
                                        ToolAction action) {

                                return ToolExecutionResult.success(
                                                "test result");
                        }
                };
        }
}