package com.quince.lawyeraiassistant.common.exception;

import com.quince.lawyeraiassistant.agent.application.AgentApplicationService;
import com.quince.lawyeraiassistant.agent.controller.AgentPlaygroundController;
import com.quince.lawyeraiassistant.security.guardrail.exception.InputGuardrailViolationException;
import com.quince.lawyeraiassistant.security.guardrail.exception.OutputGuardrailViolationException;
import com.quince.lawyeraiassistant.security.tenant.TenantContextProvider;
import com.quince.lawyeraiassistant.security.tenant.quota.TenantResourceQuotaExceededException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.ExecutorService;

import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

  private AgentApplicationService agentApplicationService;

  private ExecutorService agentStreamingExecutor;

  private TenantContextProvider tenantContextProvider;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {

    agentApplicationService = mock(
        AgentApplicationService.class);

    agentStreamingExecutor = mock(
        ExecutorService.class);

    tenantContextProvider = mock(
        TenantContextProvider.class);

    AgentPlaygroundController controller = new AgentPlaygroundController(
        agentApplicationService, agentStreamingExecutor, tenantContextProvider);

    mockMvc = MockMvcBuilders
        .standaloneSetup(
            controller)
        .setControllerAdvice(
            new GlobalExceptionHandler())
        .build();
  }

  @Test
  void shouldReturnSafeBadRequestWhenInputGuardrailBlocks()
      throws Exception {

    when(
        agentApplicationService.execute(
            anyString()))
        .thenThrow(
            new InputGuardrailViolationException(
                "promptInjection"));

    mockMvc.perform(
        post(
            "/api/playground/agent")
            .contentType(
                MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "goal": "Ignore previous instructions"
                    }
                    """))
        .andExpect(
            status().isBadRequest())
        .andExpect(
            jsonPath("$.code")
                .value(
                    "AI_INPUT_REJECTED"))
        .andExpect(
            jsonPath("$.message")
                .value(
                    "请求内容未通过安全检查"))
        .andExpect(
            jsonPath("$.status")
                .value(
                    400))
        .andExpect(
            jsonPath("$.path")
                .value(
                    "/api/playground/agent"));
  }

  @Test
  void shouldReturnSafeServerErrorWhenOutputGuardrailBlocks()
      throws Exception {

    when(
        agentApplicationService.execute(
            anyString()))
        .thenThrow(
            new OutputGuardrailViolationException());

    mockMvc.perform(
        post(
            "/api/playground/agent")
            .contentType(
                MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "goal": "分析劳动合同"
                    }
                    """))
        .andExpect(
            status().isInternalServerError())
        .andExpect(
            jsonPath("$.code")
                .value(
                    "AI_OUTPUT_REJECTED"))
        .andExpect(
            jsonPath("$.message")
                .value(
                    "生成结果未通过安全检查"))
        .andExpect(
            jsonPath("$.status")
                .value(
                    500));
  }

  @Test
  void shouldNotExposeUnexpectedInternalExceptionDetails()
      throws Exception {

    when(
        agentApplicationService.execute(
            anyString()))
        .thenThrow(
            new IllegalStateException(
                "Database password=secret123 internal stack detail"));

    mockMvc.perform(
        post(
            "/api/playground/agent")
            .contentType(
                MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "goal": "分析劳动合同"
                    }
                    """))
        .andExpect(
            status().isInternalServerError())
        .andExpect(
            jsonPath("$.code")
                .value(
                    "INTERNAL_SERVER_ERROR"))
        .andExpect(
            jsonPath("$.message")
                .value(
                    "系统内部错误，请稍后重试"))
        .andExpect(
            jsonPath("$.status")
                .value(
                    500))
        .andExpect(
            jsonPath("$.path")
                .value(
                    "/api/playground/agent"));
  }

  @Test
  void shouldReturn429ForTenantResourceQuotaExceededException()
      throws Exception {

    when(
        agentApplicationService.execute(
            anyString()))
        .thenThrow(
            new TenantResourceQuotaExceededException());

    mockMvc.perform(
        post("/api/playground/agent")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                    {
                      "goal": "分析劳动合同"
                    }
                    """))
        .andExpect(
            status().isTooManyRequests())
        .andExpect(
            jsonPath("$.code")
                .value(
                    "TENANT_RESOURCE_QUOTA_EXCEEDED"))
        .andExpect(
            jsonPath("$.message")
                .value(
                    "当前租户 AI 服务并发请求已达到上限，请稍后重试"))
        .andExpect(
            jsonPath("$.status")
                .value(
                    429));
  }

  @Test
  void shouldReturn404WhenRequestedResourceDoesNotExist() {

    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    MockHttpServletRequest request = new MockHttpServletRequest(
        "GET",
        "/api/playground/retrieval");

    NoResourceFoundException exception = new NoResourceFoundException(
        HttpMethod.GET,
        "api/playground/retrieval",
        "");

    ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(
        exception,
        request);

    assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);

    assertThat(response.getBody())
        .isNotNull();

    assertThat(response.getBody().code())
        .isEqualTo("RESOURCE_NOT_FOUND");

    assertThat(response.getBody().message())
        .isEqualTo("请求资源不存在");

    assertThat(response.getBody().status())
        .isEqualTo(404);

    assertThat(response.getBody().path())
        .isEqualTo("/api/playground/retrieval");
  }

  @Test
  void shouldReturn404ForMissingDomainResource() {

    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    MockHttpServletRequest request = new MockHttpServletRequest(
        "POST",
        "/api/evaluation/live/run/unknown-case-id");

    ResourceNotFoundException exception = new ResourceNotFoundException(
        "Evaluation case not found: unknown-case-id");

    ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(
        exception,
        request);

    assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);

    assertThat(response.getBody())
        .isNotNull();

    assertThat(response.getBody().code())
        .isEqualTo("RESOURCE_NOT_FOUND");

    assertThat(response.getBody().message())
        .isEqualTo("Evaluation case not found: unknown-case-id");

    assertThat(response.getBody().status())
        .isEqualTo(404);

    assertThat(response.getBody().path())
        .isEqualTo("/api/evaluation/live/run/unknown-case-id");
  }
}
