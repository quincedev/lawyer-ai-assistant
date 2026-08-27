package com.quince.lawyeraiassistant.evaluation.live;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.evaluation.live")
public class LiveEvaluationProperties {

    private boolean enabled = false;

    private String datasetLocation = "classpath:evaluation/legal-evaluation-dataset.json";

    private int maximumCases = 5;

    public boolean isEnabled() {

        return enabled;
    }

    public void setEnabled(
            boolean enabled) {

        this.enabled = enabled;
    }

    public String getDatasetLocation() {

        return datasetLocation;
    }

    public void setDatasetLocation(
            String datasetLocation) {

        this.datasetLocation = datasetLocation;
    }

    public int getMaximumCases() {

        return maximumCases;
    }

    public void setMaximumCases(
            int maximumCases) {

        if (maximumCases <= 0) {

            throw new IllegalArgumentException(
                    "maximumCases must be greater than zero");
        }

        this.maximumCases = maximumCases;
    }
}