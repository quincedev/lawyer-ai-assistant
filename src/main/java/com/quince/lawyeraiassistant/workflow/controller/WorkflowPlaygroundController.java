package com.quince.lawyeraiassistant.workflow.controller;

import java.util.Objects;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quince.lawyeraiassistant.agent.dto.AgentRequest;
import com.quince.lawyeraiassistant.workflow.model.WorkflowContext;
import com.quince.lawyeraiassistant.workflow.service.LegalAgentWorkflowService;

import jakarta.validation.Valid;

/**
 * 法律 Agent 工作流的开发调试接口。
 *
 * <p>
 * 该 Controller 用于从 HTTP 请求触发一条完整的法律 Agent 工作流，
 * 并将工作流执行结束后的上下文直接返回给调用方，以便在 Playground
 * 环境中观察节点状态、流程变量及最终执行结果。
 * </p>
 *
 * <p>
 * 此接口面向开发和联调场景，不属于正式的业务 API。
 * </p>
 */
@RestController
@RequestMapping("/api/playground/")
public class WorkflowPlaygroundController {

    private final LegalAgentWorkflowService legalAgentWorkflowService;

    /**
     * 创建工作流 Playground Controller。
     *
     * @param legalAgentWorkflowService 法律 Agent 工作流编排服务
     * @throws NullPointerException 当工作流服务为 {@code null} 时抛出
     */
    public WorkflowPlaygroundController(
            LegalAgentWorkflowService legalAgentWorkflowService) {

        this.legalAgentWorkflowService = Objects.requireNonNull(
                legalAgentWorkflowService,
                "legalAgentWorkflowService must not be null");
    }

    /**
     * 根据请求中的目标执行法律 Agent 工作流。
     *
     * @param request 包含工作流执行目标的 Agent 请求
     * @return 工作流执行完成后的上下文
     */
    @PostMapping("/workflow")
    public WorkflowContext runWorkflow(
            @Valid @RequestBody AgentRequest request) {

        return legalAgentWorkflowService.execute(
                request.goal());
    }
}
