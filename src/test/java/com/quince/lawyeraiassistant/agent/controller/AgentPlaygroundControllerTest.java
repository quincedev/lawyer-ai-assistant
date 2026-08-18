package com.quince.lawyeraiassistant.agent.controller;

import com.quince.lawyeraiassistant.agent.application.AgentApplicationService;
import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentPlan;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.AgentTaskStatus;
import com.quince.lawyeraiassistant.agent.model.ReasonResult;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentPlaygroundControllerTest {

        private AgentApplicationService agentApplicationService;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {

                agentApplicationService = mock(
                                AgentApplicationService.class);

                AgentPlaygroundController controller = new AgentPlaygroundController(
                                agentApplicationService);

                mockMvc = MockMvcBuilders
                                .standaloneSetup(
                                                controller)
                                .build();
        }

        @Test
        void shouldRunAgentApplicationServiceAndReturnResponse()
                        throws Exception {

                String goal = "查询违法解除劳动合同需要承担什么法律责任";

                AgentContext resultContext = AgentContext.builder()
                                .goal(
                                                goal)
                                .reasonResult(
                                                ReasonResult.from(
                                                                "用户希望了解违法解除劳动合同所对应的法律责任。"))
                                .agentPlan(
                                                AgentPlan.from(
                                                                List.of(
                                                                                AgentTask.pending(
                                                                                                "task-1",
                                                                                                "查询劳动合同法相关规定")
                                                                                                .withStatus(
                                                                                                                AgentTaskStatus.COMPLETED),
                                                                                AgentTask.pending(
                                                                                                "task-2",
                                                                                                "查询违法解除的法律责任")
                                                                                                .withStatus(
                                                                                                                AgentTaskStatus.COMPLETED))))
                                .observations(
                                                List.of(
                                                                ToolObservation.success(
                                                                                "task-1",
                                                                                "searchLegalKnowledge",
                                                                                "检索到劳动合同法相关规定。"),
                                                                ToolObservation.success(
                                                                                "task-2",
                                                                                "searchLegalKnowledge",
                                                                                "检索到违法解除劳动合同的法律责任。")))
                                .status(
                                                AgentStatus.FINISHED)
                                .executionLogs(
                                                List.of(
                                                                "Reason completed",
                                                                "Planning completed",
                                                                "Tool execution completed: task-1",
                                                                "Tool execution completed: task-2",
                                                                "Agent finished"))
                                .build();

                when(
                                agentApplicationService.execute(
                                                goal))
                                .thenReturn(
                                                resultContext);

                String requestBody = """
                                {
                                    "goal": "查询违法解除劳动合同需要承担什么法律责任"
                                }
                                """;

                mockMvc.perform(
                                post(
                                                "/api/playground/agent")
                                                .contentType(
                                                                MediaType.APPLICATION_JSON)
                                                .content(
                                                                requestBody))
                                .andExpect(
                                                status().isOk())
                                .andExpect(
                                                jsonPath("$.goal")
                                                                .value(
                                                                                goal))
                                .andExpect(
                                                jsonPath("$.reasonSummary")
                                                                .value(
                                                                                "用户希望了解违法解除劳动合同所对应的法律责任。"))
                                .andExpect(
                                                jsonPath("$.plan.length()")
                                                                .value(2))
                                .andExpect(
                                                jsonPath("$.plan[0].id")
                                                                .value(
                                                                                "task-1"))
                                .andExpect(
                                                jsonPath("$.plan[0].status")
                                                                .value(
                                                                                "COMPLETED"))
                                .andExpect(
                                                jsonPath("$.plan[1].id")
                                                                .value(
                                                                                "task-2"))
                                .andExpect(
                                                jsonPath("$.plan[1].status")
                                                                .value(
                                                                                "COMPLETED"))
                                .andExpect(
                                                jsonPath("$.observations.length()")
                                                                .value(2))
                                .andExpect(
                                                jsonPath("$.observations[0].toolName")
                                                                .value(
                                                                                "searchLegalKnowledge"))
                                .andExpect(
                                                jsonPath("$.observations[0].success")
                                                                .value(true))
                                .andExpect(
                                                jsonPath("$.status")
                                                                .value(
                                                                                "FINISHED"))
                                .andExpect(
                                                jsonPath("$.executionLogs[4]")
                                                                .value(
                                                                                "Agent finished"));

                verify(
                                agentApplicationService)
                                .execute(
                                                goal);
        }
}