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
class AgentWarmPerformanceRegressionTest
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
         * 每个 Warm Regression Test
         * 自己建立 Warm 状态，
         * 不依赖其他 Test 的执行顺序。
         */
        toolResultCache.clear();
    }

    @Test
    void warmExecutionShouldStayWithinPerformanceBaseline() {

        /*
         * =================================================
         * 1. Warm-up
         * =================================================
         */

        AgentContext warmUpContext = buildTenantAContext();

        AgentContext warmUpResult = agentRuntime.run(
                warmUpContext);

        assertEquals(
                AgentStatus.FINISHED,
                warmUpResult.getStatus());

        /*
         * Warm-up Snapshot 不参与最终 Regression。
         */
        snapshotRecorder.clear();

        /*
         * =================================================
         * 2. Actual Warm Benchmark
         * =================================================
         */

        AgentContext warmContext = buildTenantAContext();

        AgentContext result = agentRuntime.run(
                warmContext);

        assertEquals(
                AgentStatus.FINISHED,
                result.getStatus());

        AgentPerformanceSnapshot snapshot = snapshotRecorder.requireLatest();

        AgentPerformanceRegressionReport report = regressionEvaluator.evaluate(
                snapshot,
                warmBaseline());

        printReport(
                "WARM",
                report);

        assertTrue(
                report.passed(),
                () -> "Warm performance regression detected:"
                        + System.lineSeparator()
                        + formatReport(
                                report));
    }
}
