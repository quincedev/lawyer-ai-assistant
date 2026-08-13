package com.quince.lawyeraiassistant.workflow.model;

/**
 * Workflow 整体运行状态。
 */
public enum WorkflowStatus {

    /**
     * Workflow 已创建，但尚未开始。
     */
    PENDING,

    /**
     * Workflow 正在运行。
     */
    RUNNING,

    /**
     * Workflow 正常执行完成。
     */
    COMPLETED,

    /**
     * Workflow 执行失败。
     */
    FAILED
}