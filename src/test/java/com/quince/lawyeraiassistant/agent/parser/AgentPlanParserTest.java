package com.quince.lawyeraiassistant.agent.parser;

import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.AgentTaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentPlanParserTest {

    private AgentPlanParser parser;

    @BeforeEach
    void setUp() {

        parser = new AgentPlanParser();
    }

    @Test
    void shouldParseAgentPlan() {

        String content = """
                task-1|读取劳动合同
                task-2|识别法律风险
                task-3|生成律师意见书
                """;

        AgentPlan result = parser.parse(
                content);

        assertEquals(
                3,
                result.taskCount());

        assertEquals(
                "task-1",
                result.getTasks()
                        .get(0)
                        .getId());

        assertEquals(
                "读取劳动合同",
                result.getTasks()
                        .get(0)
                        .getDescription());

        assertEquals(
                AgentTaskStatus.PENDING,
                result.getTasks()
                        .get(0)
                        .getStatus());

        assertEquals(
                "task-3",
                result.getTasks()
                        .get(2)
                        .getId());
    }

    @Test
    void shouldIgnoreBlankLines() {

        String content = """

                task-1|读取劳动合同

                task-2|识别法律风险

                """;

        AgentPlan result = parser.parse(
                content);

        assertEquals(
                2,
                result.taskCount());
    }

    @Test
    void shouldTrimTaskFields() {

        AgentPlan result = parser.parse(
                "  task-1  |  读取劳动合同  ");

        assertEquals(
                "task-1",
                result.getTasks()
                        .get(0)
                        .getId());

        assertEquals(
                "读取劳动合同",
                result.getTasks()
                        .get(0)
                        .getDescription());
    }

    @Test
    void shouldPreservePipeInsideDescription() {

        AgentPlan result = parser.parse(
                "task-1|分析 A | B 条款");

        assertEquals(
                "分析 A | B 条款",
                result.getTasks()
                        .get(0)
                        .getDescription());
    }

    @Test
    void shouldRejectNullContent() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> parser.parse(
                        null));

        assertEquals(
                "Agent plan content must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankContent() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(
                        "   "));

        assertEquals(
                "Agent plan content must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldRejectInvalidTaskFormat() {

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> parser.parse(
                        "读取劳动合同"));

        assertEquals(
                "Invalid agent plan task format: 读取劳动合同",
                exception.getMessage());
    }

    @Test
    void shouldRejectEmptyTaskDescription() {

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> parser.parse(
                        "task-1|   "));

        assertEquals(
                "Invalid agent plan task: task-1|",
                exception.getMessage());
    }

    @Test
    void shouldRejectEmptyTaskId() {

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> parser.parse(
                        "|读取劳动合同"));

        assertEquals(
                "Invalid agent plan task: |读取劳动合同",
                exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateTaskIds() {

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> parser.parse(
                        """
                                task-1|读取劳动合同
                                task-1|识别法律风险
                                """));

        assertEquals(
                "Invalid agent plan",
                exception.getMessage());
    }
}