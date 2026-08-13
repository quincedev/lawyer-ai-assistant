package com.quince.lawyeraiassistant.workflow.node;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowNodeTest {

    @Test
    void shouldCreateWorkflowNode() {

        WorkflowNode node = WorkflowNode.of(
                "parse-contract",
                "Parse Contract",
                "解析用户上传的合同");

        assertEquals(
                "parse-contract",
                node.getId());

        assertEquals(
                "Parse Contract",
                node.getName());

        assertEquals(
                "解析用户上传的合同",
                node.getDescription());
    }

    @Test
    void shouldNormalizeWorkflowNodeFields() {

        WorkflowNode node = WorkflowNode.of(
                "  parse-contract  ",
                "  Parse Contract  ",
                "  解析合同  ");

        assertEquals(
                "parse-contract",
                node.getId());

        assertEquals(
                "Parse Contract",
                node.getName());

        assertEquals(
                "解析合同",
                node.getDescription());
    }

    @Test
    void shouldUseEmptyDescriptionWhenDescriptionIsNull() {

        WorkflowNode node = WorkflowNode.of(
                "parse-contract",
                "Parse Contract",
                null);

        assertEquals(
                "",
                node.getDescription());
    }

    @Test
    void shouldUseEmptyDescriptionWhenDescriptionIsBlank() {

        WorkflowNode node = WorkflowNode.of(
                "parse-contract",
                "Parse Contract",
                "   ");

        assertEquals(
                "",
                node.getDescription());
    }

    @Test
    void shouldRejectNullId() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> WorkflowNode.of(
                        null,
                        "Parse Contract",
                        "解析合同"));

        assertEquals(
                "Node id must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankId() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> WorkflowNode.of(
                        "   ",
                        "Parse Contract",
                        "解析合同"));

        assertEquals(
                "Node id must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullName() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> WorkflowNode.of(
                        "parse-contract",
                        null,
                        "解析合同"));

        assertEquals(
                "Node name must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankName() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> WorkflowNode.of(
                        "parse-contract",
                        "   ",
                        "解析合同"));

        assertEquals(
                "Node name must not be blank",
                exception.getMessage());
    }

    @Test
    void shouldCreateStandardWorkflowNodeByDefault() {

        WorkflowNode node = WorkflowNode.of(
                "prepare-request",
                "Prepare Request",
                null);

        assertEquals(
                WorkflowNodeType.STANDARD,
                node.getType());
    }

    @Test
    void shouldCreateAgentWorkflowNode() {

        WorkflowNode node = WorkflowNode.agent(
                "legal-agent",
                "Legal Agent",
                "执行 AI Agent");

        assertEquals(
                "legal-agent",
                node.getId());

        assertEquals(
                WorkflowNodeType.AGENT,
                node.getType());
    }
    
}