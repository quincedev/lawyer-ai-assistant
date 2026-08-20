package com.quince.lawyeraiassistant.agent.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentStatus;
import com.quince.lawyeraiassistant.agent.runtime.AgentRuntime;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshot;
import com.quince.lawyeraiassistant.agent.runtime.metrics.AgentPerformanceSnapshotRecorder;
import com.quince.lawyeraiassistant.agent.runtime.performance.regression.AgentPerformanceRegressionEvaluator;
import com.quince.lawyeraiassistant.agent.runtime.performance.regression.AgentPerformanceRegressionReport;
import com.quince.lawyeraiassistant.cache.tool.ToolResultCache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Tag("performance-regression")
@SpringBootTest
@ActiveProfiles("mcp-agent")
@EnabledIfSystemProperty(
        named = "agent.performance-regression.enabled",
        matches = "true")
class AgentColdPerformanceRegressionTest
        extends AgentPerformanceRegressionTestSupport {

    @Autowired
    private AgentRuntime agentRuntime;

    @Autowired
    private AgentPerformanceSnapshotRecorder snapshotRecorder;

    @Autowired
    private AgentPerformanceRegressionEvaluator regressionEvaluator;

    @Autowired
    private ToolResultCache toolResultCache;

    @BeforeEach
    void setUp() {

        snapshotRecorder.clear();

        /*
         * 8D Cold 的定义：
         *
         * Agent Tool Cache Cold。
         *
         * MCP Server 自己的 RAG Cache
         * 不属于这里的 Release Gate 范围。
         */
        toolResultCache.clear();
    }

    @Test
    void coldExecutionShouldStayWithinPerformanceBaseline() {

        AgentContext context = buildTenantAContext();

        AgentContext result = agentRuntime.run(
                context);

        assertEquals(
                AgentStatus.FINISHED,
                result.getStatus());

        AgentPerformanceSnapshot snapshot = snapshotRecorder.requireLatest();

        AgentPerformanceRegressionReport report = regressionEvaluator.evaluate(
                snapshot,
                coldBaseline());

        printReport(
                "COLD",
                report);

        assertTrue(
                report.passed(),
                () -> "Cold performance regression detected:"
                        + System.lineSeparator()
                        + formatReport(
                                report));
    }
}
