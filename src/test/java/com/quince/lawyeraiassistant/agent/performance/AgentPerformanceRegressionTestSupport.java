package com.quince.lawyeraiassistant.agent.performance;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.runtime.performance.regression.AgentPerformanceBaseline;
import com.quince.lawyeraiassistant.agent.runtime.performance.regression.AgentPerformanceRegressionReport;
import com.quince.lawyeraiassistant.agent.runtime.performance.regression.PerformanceRegressionMetricResult;
import com.quince.lawyeraiassistant.security.identity.UserRole;
import com.quince.lawyeraiassistant.security.tenant.TenantContext;

import java.util.Set;

abstract class AgentPerformanceRegressionTestSupport {

    protected static final String GOAL = "分析劳动合同违法解除的主要法律责任";

    protected AgentPerformanceBaseline coldBaseline() {

        return new AgentPerformanceBaseline(
                13,
                3,
                1,
                1,
                1,
                0.50,
                0.55,
                120_000,
                1.30,
                1.75);
    }

    protected AgentPerformanceBaseline warmBaseline() {

        return new AgentPerformanceBaseline(
                13,
                2,
                0,
                1,
                1,
                0.90,
                0.55,
                120_000,
                1.30,
                1.75);
    }

    protected AgentContext buildTenantAContext() {

        TenantContext tenantContext = new TenantContext(
                "tenant-a",
                "user-001",
                "quince",
                Set.of(
                        UserRole.LAWYER));

        return AgentContext.authenticated(
                GOAL,
                tenantContext);
    }

    protected String formatReport(
            AgentPerformanceRegressionReport report) {

        StringBuilder builder = new StringBuilder();

        builder.append(
                "Overall: ")
                .append(
                        report.overallLevel())
                .append(
                        System.lineSeparator());

        for (PerformanceRegressionMetricResult result : report.results()) {

            builder.append(
                    result.metric())
                    .append(
                            " -> ")
                    .append(
                            result.level())
                    .append(
                            ", actual=")
                    .append(
                            result.actual())
                    .append(
                            ", baseline=")
                    .append(
                            result.baseline())
                    .append(
                            ", reason=")
                    .append(
                            result.reason())
                    .append(
                            System.lineSeparator());
        }

        return builder.toString();
    }

    protected void printReport(
            String mode,
            AgentPerformanceRegressionReport report) {

        System.out.println();
        System.out.println(
                "======================================");

        System.out.println(
                "AGENT PERFORMANCE REGRESSION - "
                        + mode);

        System.out.println(
                "======================================");

        System.out.println(
                formatReport(
                        report));
    }
}