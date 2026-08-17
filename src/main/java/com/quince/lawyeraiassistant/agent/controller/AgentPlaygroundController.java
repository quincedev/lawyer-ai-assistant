package com.quince.lawyeraiassistant.agent.controller;

import com.quince.lawyeraiassistant.agent.application.AgentApplicationService;
import com.quince.lawyeraiassistant.agent.dto.AgentRequest;
import com.quince.lawyeraiassistant.agent.dto.AgentResponse;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Agent Pipeline 开发诊断接口。
 *
 * <p>
 * 用于验证 Agent Runtime 的完整执行流程，
 * 包括 Reason、Planning、Action Selection、
 * Tool Execution、Observation 和 Multi-step Execution。
 * </p>
 *
 * <p>
 * 当前接口属于 Playground / Development API，
 * 用于 Agent Runtime 开发和联调，
 * 不作为最终业务 API。
 * </p>
 */
@RestController
@RequestMapping("/api/playground")
public class AgentPlaygroundController {

        private final AgentApplicationService agentApplicationService;

        public AgentPlaygroundController(
                        AgentApplicationService agentApplicationService) {

                this.agentApplicationService = Objects.requireNonNull(
                                agentApplicationService,
                                "agentApplicationService must not be null");
        }

        @PostMapping("/agent")
        public AgentResponse run(
                        @Valid @RequestBody AgentRequest request) {

                AgentContext result = agentApplicationService.execute(
                                request.goal());

                return AgentResponse.from(
                                result);
        }
}