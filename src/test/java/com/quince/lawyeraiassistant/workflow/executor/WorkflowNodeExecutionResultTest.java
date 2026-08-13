package com.quince.lawyeraiassistant.workflow.executor;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowNodeExecutionResultTest {

    @Test
    void shouldCreateSuccessResultWithoutVariables() {

        WorkflowNodeExecutionResult result = WorkflowNodeExecutionResult.success();

        assertTrue(
                result.isSuccess());

        assertTrue(
                result.getVariables()
                        .isEmpty());

        assertEquals(
                "",
                result.getErrorMessage());
    }

    @Test
    void shouldCreateSuccessResultWithVariables() {

        WorkflowNodeExecutionResult result = WorkflowNodeExecutionResult.success(
                Map.of(
                        "riskLevel",
                        "HIGH"));

        assertTrue(
                result.isSuccess());

        assertEquals(
                "HIGH",
                result.getVariables()
                        .get(
                                "riskLevel"));

        assertEquals(
                "",
                result.getErrorMessage());
    }

    @Test
    void shouldUseEmptyVariablesWhenSuccessVariablesIsNull() {

        WorkflowNodeExecutionResult result = WorkflowNodeExecutionResult.success(
                null);

        assertTrue(
                result.isSuccess());

        assertTrue(
                result.getVariables()
                        .isEmpty());
    }

    @Test
    void shouldCreateFailureResult() {

        WorkflowNodeExecutionResult result = WorkflowNodeExecutionResult.failure(
                "节点执行失败");

        assertFalse(
                result.isSuccess());

        assertTrue(
                result.getVariables()
                        .isEmpty());

        assertEquals(
                "节点执行失败",
                result.getErrorMessage());
    }

    @Test
    void shouldNormalizeFailureMessage() {

        WorkflowNodeExecutionResult result = WorkflowNodeExecutionResult.failure(
                "  节点执行失败  ");

        assertEquals(
                "节点执行失败",
                result.getErrorMessage());
    }

    @Test
    void shouldRejectNullFailureMessage() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> WorkflowNodeExecutionResult.failure(
                        null));

        assertEquals(
                "Error message must not be null",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankFailureMessage() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> WorkflowNodeExecutionResult.failure(
                        "   "));

        assertEquals(
                "Error message must not be blank",
                exception.getMessage());
    }
}