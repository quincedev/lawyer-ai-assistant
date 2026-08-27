package com.quince.lawyeraiassistant.evaluation.live;

import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCase;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "app.evaluation.live", name = "enabled", havingValue = "true")
public class ClasspathAgentEvaluationDataset
        implements AgentEvaluationDataset {

    private final ObjectMapper objectMapper;

    private final ResourceLoader resourceLoader;

    private final LiveEvaluationProperties properties;

    public ClasspathAgentEvaluationDataset(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            LiveEvaluationProperties properties) {

        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper must not be null");

        this.resourceLoader = Objects.requireNonNull(
                resourceLoader,
                "resourceLoader must not be null");

        this.properties = Objects.requireNonNull(
                properties,
                "properties must not be null");
    }

    @Override
    public List<AgentEvaluationCase> load() {

        Resource resource = resourceLoader.getResource(
                properties.getDatasetLocation());

        if (!resource.exists()) {

            throw new IllegalStateException(
                    "Evaluation dataset does not exist: "
                            + properties.getDatasetLocation());
        }

        try (InputStream inputStream = resource.getInputStream()) {

            AgentEvaluationCase[] values = objectMapper.readValue(
                    inputStream,
                    AgentEvaluationCase[].class);

            List<AgentEvaluationCase> cases = List.copyOf(
                    Arrays.asList(values));

            validate(cases);

            return cases;

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to load evaluation dataset: "
                            + properties.getDatasetLocation(),
                    exception);
        }
    }

    private void validate(
            List<AgentEvaluationCase> cases) {

        if (cases.isEmpty()) {

            throw new IllegalStateException(
                    "Evaluation dataset must not be empty");
        }

        Set<String> caseIds = new HashSet<>();

        for (AgentEvaluationCase evaluationCase : cases) {

            if (!caseIds.add(
                    evaluationCase.caseId())) {

                throw new IllegalStateException(
                        "Duplicate evaluation caseId: "
                                + evaluationCase.caseId());
            }
        }
    }
}