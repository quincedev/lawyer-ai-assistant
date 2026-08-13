package com.quince.lawyeraiassistant.workflow.model;

/**
 * Workflow Node 的运行状态。
 */
public enum WorkflowNodeStatus {

    /**
     * 尚未执行。
     */
    PENDING,

    /**
     * 正在执行。
     */
    RUNNING,

    /**
     * 执行成功。
     */
    COMPLETED,

    /**
     * 执行失败。
     */
    FAILED,

    /**
     * 当前 Node 被跳过。
     */
    SKIPPED
}