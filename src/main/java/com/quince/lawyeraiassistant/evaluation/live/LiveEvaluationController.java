package com.quince.lawyeraiassistant.evaluation.live;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@Profile("!mcp-server")
@ConditionalOnProperty(prefix = "app.evaluation.live", name = "enabled", havingValue = "true")
@RequestMapping("/api/evaluation/live")
public class LiveEvaluationController {

    private final LiveAgentEvaluationRunner runner;

    public LiveEvaluationController(
            LiveAgentEvaluationRunner runner) {

        this.runner = Objects.requireNonNull(
                runner,
                "runner must not be null");
    }

    @PostMapping("/run")
    public LiveEvaluationReport runAll() {

        return runner.runAll();
    }

    @PostMapping("/run/{caseId}")
    public LiveEvaluationReport runCase(
            @PathVariable String caseId) {

        return runner.runCase(
                caseId);
    }
}