package com.quince.lawyeraiassistant.prompt.definition;

import com.quince.lawyeraiassistant.prompt.PromptNames;
import com.quince.lawyeraiassistant.prompt.PromptPaths;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromptDefinitionTest {

    @Test
    void shouldDefineAgentFinalAnswerPrompt() {

        PromptDefinition definition =
                PromptDefinition.AGENT_FINAL_ANSWER;

        assertEquals(
                PromptNames.AGENT_FINAL_ANSWER,
                definition.getName());

        assertEquals(
                PromptPaths.AGENT_FINAL_ANSWER,
                definition.getLocation());

        assertEquals(
                "v1",
                definition.getVersion());
    }
}