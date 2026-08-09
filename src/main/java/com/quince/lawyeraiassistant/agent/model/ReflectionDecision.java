package com.quince.lawyeraiassistant.agent.model;

public enum ReflectionDecision {

    /**
     * 当前任务已经满足要求，
     * 可以继续执行后续任务。
     */
    CONTINUE,

    /**
     * 当前 Action 的结果不足，
     * 但原 Task / Plan 没有问题。
     */
    RETRY,

    /**
     * 当前执行结果说明原 Plan
     * 已经不再适合，需要重新规划。
     */
    REPLAN,

    /**
     * 当前信息已经足够完成整个 Goal。
     */
    FINISH
}