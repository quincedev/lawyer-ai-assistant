package com.quince.lawyeraiassistant.evaluation.judge.metrics;

import com.quince.lawyeraiassistant.evaluation.judge.ModelJudgeResult;
import com.quince.lawyeraiassistant.evaluation.judge.ModelJudgeStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

@Component
public class MicrometerModelJudgeMetrics implements ModelJudgeMetrics {

    private final Counter passedCalls;
    private final Counter failedCalls;
    private final Counter technicalFailures;
    private final DistributionSummary score;
    private final Timer duration;
    private final Counter promptTokens;
    private final Counter completionTokens;
    private final Counter totalTokens;
    private final Counter usageMissing;
    private final Counter costUsd;
    private final Counter pricingMissing;

    public MicrometerModelJudgeMetrics(MeterRegistry meterRegistry) {
        Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        passedCalls = counter(meterRegistry, "agent.evaluation.judge.calls",
                "Completed model judge evaluations", "result", "pass");
        failedCalls = counter(meterRegistry, "agent.evaluation.judge.calls",
                "Completed model judge evaluations", "result", "fail");
        technicalFailures = counter(meterRegistry, "agent.evaluation.judge.failures",
                "Model judge technical failures");
        score = DistributionSummary.builder("agent.evaluation.judge.score")
                .description("Model judge overall score from 0 to 100")
                .maximumExpectedValue(100.0D).register(meterRegistry);
        duration = Timer.builder("agent.evaluation.judge.duration")
                .description("Model judge evaluation duration")
                .publishPercentileHistogram().register(meterRegistry);
        promptTokens = counter(meterRegistry, "agent.evaluation.judge.prompt.tokens",
                "Model judge prompt tokens");
        completionTokens = counter(meterRegistry, "agent.evaluation.judge.completion.tokens",
                "Model judge completion tokens");
        totalTokens = counter(meterRegistry, "agent.evaluation.judge.total.tokens",
                "Model judge total tokens");
        usageMissing = counter(meterRegistry, "agent.evaluation.judge.usage.missing",
                "Model judge calls without complete token usage");
        costUsd = counter(meterRegistry, "agent.evaluation.judge.cost.usd",
                "Estimated model judge provider API cost in USD");
        pricingMissing = counter(meterRegistry, "agent.evaluation.judge.pricing.missing",
                "Model judge calls with usage but without pricing");
    }

    @Override
    public void record(ModelJudgeResult result) {
        Objects.requireNonNull(result, "result must not be null");
        if (result.status() == ModelJudgeStatus.DISABLED
                || result.status() == ModelJudgeStatus.SKIPPED) {
            return;
        }
        duration.record(Duration.ofMillis(Math.max(0, result.durationMs())));
        if (result.status() == ModelJudgeStatus.FAILED) {
            technicalFailures.increment();
            return;
        }
        score.record(normalizeScore(result.overallScore()));
        if (result.passed()) {
            passedCalls.increment();
        } else {
            failedCalls.increment();
        }
        if (!result.usageAvailable()) {
            usageMissing.increment();
            return;
        }
        promptTokens.increment(result.promptTokens());
        completionTokens.increment(result.completionTokens());
        totalTokens.increment(result.totalTokens());
        if (!result.pricingAvailable()) {
            pricingMissing.increment();
            return;
        }
        BigDecimal cost = result.costUsd();
        if (cost.signum() > 0) {
            costUsd.increment(cost.doubleValue());
        }
    }

    private Counter counter(MeterRegistry registry, String name, String description,
            String... tags) {
        return Counter.builder(name).description(description).tags(tags).register(registry);
    }

    private double normalizeScore(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, value));
    }
}
