package com.quince.lawyeraiassistant.evaluation.live;

import com.quince.lawyeraiassistant.evaluation.model.AgentEvaluationCase;

import java.util.List;
import java.util.Optional;

public interface AgentEvaluationDataset {

    List<AgentEvaluationCase> load();

    default Optional<AgentEvaluationCase> findById(
            String caseId) {

        if (caseId == null
                || caseId.isBlank()) {

            return Optional.empty();
        }

        return load()
                .stream()
                .filter(evaluationCase -> evaluationCase.caseId()
                        .equals(caseId.trim()))
                .findFirst();
    }
}