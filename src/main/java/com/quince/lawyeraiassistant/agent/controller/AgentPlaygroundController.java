package com.quince.lawyeraiassistant.agent.controller;

import com.quince.lawyeraiassistant.agent.application.AgentApplicationService;
import com.quince.lawyeraiassistant.agent.dto.AgentRequest;
import com.quince.lawyeraiassistant.agent.dto.AgentResponse;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.runtime.DefaultAgentRuntime;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;
import com.quince.lawyeraiassistant.security.tenant.TenantContextProvider;
import com.quince.lawyeraiassistant.web.stream.SseAgentStreamPublisher;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

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

        private final ExecutorService agentStreamingExecutor;

        private final TenantContextProvider tenantContextProvider;

        private static final Logger log = LoggerFactory.getLogger(
                        DefaultAgentRuntime.class);

        public AgentPlaygroundController(
                        AgentApplicationService agentApplicationService,
                        @Qualifier("agentStreamingExecutor") ExecutorService agentStreamingExecutor,
                        TenantContextProvider tenantContextProvider) {

                this.agentApplicationService = Objects.requireNonNull(
                                agentApplicationService,
                                "agentApplicationService must not be null");

                this.agentStreamingExecutor = Objects.requireNonNull(
                                agentStreamingExecutor,
                                "agentStreamingExecutor must not be null");

                this.tenantContextProvider = Objects.requireNonNull(
                                tenantContextProvider,
                                "tenantContextProvider must not be null");
        }

        @PostMapping("/agent")
        public AgentResponse run(
                        @Valid @RequestBody AgentRequest request) {

                AgentContext result = agentApplicationService.execute(
                                request.goal());

                return AgentResponse.from(
                                result);
        }

        @PostMapping(value = "/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public SseEmitter stream(
                        @Valid @RequestBody AgentRequest request) {

                /*
                 * 这里仍在 HTTP request thread。
                 *
                 * JWT Authentication / SecurityContext
                 * 仍然有效，所以必须在这里读取可信 TenantContext。
                 */
                TenantContext tenantContext = tenantContextProvider.current();

                SseEmitter emitter = new SseEmitter(
                                0L);

                SseAgentStreamPublisher publisher = new SseAgentStreamPublisher(
                                emitter);

                agentStreamingExecutor.submit(
                                () -> {

                                        try {

                                                agentApplicationService.executeStreaming(
                                                                request.goal(),
                                                                tenantContext,
                                                                publisher);

                                                emitter.complete();

                                        } catch (RuntimeException exception) {

                                                log.error(
                                                                "Streaming agent execution failed",
                                                                exception);

                                                emitter.complete();
                                        }
                                });

                return emitter;
        }
}