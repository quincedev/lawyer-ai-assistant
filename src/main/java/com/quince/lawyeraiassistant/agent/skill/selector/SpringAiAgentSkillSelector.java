package com.quince.lawyeraiassistant.agent.skill.selector;

import com.quince.lawyeraiassistant.agent.skill.AgentSkill;
import com.quince.lawyeraiassistant.agent.skill.AgentSkillRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 基于 LLM 的 Skill Selector。
 *
 * <p>
 * 根据用户 Goal 和当前 Skill Registry 中的 Capability Catalog，
 * 选择最适合当前目标的 Skill。
 * </p>
 */
public class SpringAiAgentSkillSelector
        implements AgentSkillSelector {

    private static final String NONE = "NONE";

    private final ChatClient chatClient;

    private final AgentSkillRegistry skillRegistry;

    private final Resource promptResource;

    public SpringAiAgentSkillSelector(
            ChatClient chatClient,
            AgentSkillRegistry skillRegistry,
            Resource promptResource) {

        this.chatClient = Objects.requireNonNull(
                chatClient,
                "ChatClient must not be null");

        this.skillRegistry = Objects.requireNonNull(
                skillRegistry,
                "AgentSkillRegistry must not be null");

        this.promptResource = Objects.requireNonNull(
                promptResource,
                "Skill selection prompt resource must not be null");
    }

    @Override
    public Optional<AgentSkill> select(
            String goal) {

        String normalizedGoal = requireGoal(
                goal);

        List<AgentSkill> availableSkills = skillRegistry.list();

        if (availableSkills.isEmpty()) {
            return Optional.empty();
        }

        String skillCatalog = buildSkillCatalog(
                availableSkills);

        SkillSelectionResponse response = chatClient
                .prompt()
                .user(
                        user -> user.text(
                                promptResource)
                                .param(
                                        "goal",
                                        normalizedGoal)
                                .param(
                                        "availableSkills",
                                        skillCatalog))
                .call()
                .entity(
                        SkillSelectionResponse.class);

        return resolveSelectedSkill(
                response);
    }

    private Optional<AgentSkill> resolveSelectedSkill(
            SkillSelectionResponse response) {

        if (response == null
                || response.skillId() == null
                || response.skillId().isBlank()) {

            return Optional.empty();
        }

        String skillId = response.skillId()
                .trim();

        if (NONE.equalsIgnoreCase(
                skillId)) {

            return Optional.empty();
        }

        return skillRegistry.findById(
                skillId);
    }

    private String buildSkillCatalog(
            List<AgentSkill> skills) {

        return skills.stream()
                .map(
                        this::formatSkill)
                .collect(
                        Collectors.joining(
                                "\n\n"));
    }

    private String formatSkill(
            AgentSkill skill) {

        String tags = skill.getTags()
                .isEmpty()
                        ? "(none)"
                        : String.join(
                                ", ",
                                skill.getTags());

        return """
                ID: %s
                Name: %s
                Description: %s
                Tags: %s
                """
                .formatted(
                        skill.getId(),
                        skill.getName(),
                        skill.getDescription(),
                        tags)
                .trim();
    }

    private String requireGoal(
            String goal) {

        if (goal == null
                || goal.isBlank()) {

            throw new IllegalArgumentException(
                    "Goal must not be blank");
        }

        return goal.trim();
    }
}