package com.quince.lawyeraiassistant.agent.skill.selector;

import com.quince.lawyeraiassistant.agent.skill.AgentSkill;
import com.quince.lawyeraiassistant.agent.skill.AgentSkillRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiAgentSkillSelectorTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private Resource promptResource;

    private AgentSkill legalResearchSkill;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(
                this);

        legalResearchSkill = AgentSkill.of(
                "legal-research",
                "Legal Research",
                "用于研究具体法律问题并形成法律分析",
                "执行法律研究",
                List.of(
                        "searchLegalKnowledge"),
                Set.of(
                        "legal",
                        "research"));
    }

    @Test
    void shouldSelectSkillReturnedByModel() {

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of(
                        legalResearchSkill));

        SpringAiAgentSkillSelector selector = createSelector(
                registry);

        mockChatResponse(
                new SkillSelectionResponse(
                        "legal-research"));

        Optional<AgentSkill> result = selector.select(
                "研究违法解除劳动合同的法律责任");

        assertTrue(
                result.isPresent());

        assertEquals(
                "legal-research",
                result.orElseThrow()
                        .getId());
    }

    @Test
    void shouldReturnEmptyWhenModelReturnsNone() {

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of(
                        legalResearchSkill));

        SpringAiAgentSkillSelector selector = createSelector(
                registry);

        mockChatResponse(
                new SkillSelectionResponse(
                        "NONE"));

        Optional<AgentSkill> result = selector.select(
                "你好");

        assertTrue(
                result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenModelReturnsUnknownSkill() {

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of(
                        legalResearchSkill));

        SpringAiAgentSkillSelector selector = createSelector(
                registry);

        mockChatResponse(
                new SkillSelectionResponse(
                        "unknown-skill"));

        Optional<AgentSkill> result = selector.select(
                "研究劳动合同问题");

        assertTrue(
                result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenResponseSkillIdIsBlank() {

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of(
                        legalResearchSkill));

        SpringAiAgentSkillSelector selector = createSelector(
                registry);

        mockChatResponse(
                new SkillSelectionResponse(
                        " "));

        Optional<AgentSkill> result = selector.select(
                "研究劳动合同问题");

        assertTrue(
                result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWithoutCallingModelWhenRegistryIsEmpty() {

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of());

        SpringAiAgentSkillSelector selector = createSelector(
                registry);

        Optional<AgentSkill> result = selector.select(
                "研究劳动合同问题");

        assertTrue(
                result.isEmpty());

        verify(
                chatClient,
                never())
                .prompt();
    }

    @Test
    void shouldRejectBlankGoal() {

        AgentSkillRegistry registry = new AgentSkillRegistry(
                List.of(
                        legalResearchSkill));

        SpringAiAgentSkillSelector selector = createSelector(
                registry);

        assertThrows(
                IllegalArgumentException.class,
                () -> selector.select(
                        " "));
    }

    private SpringAiAgentSkillSelector createSelector(
            AgentSkillRegistry registry) {

        return new SpringAiAgentSkillSelector(
                chatClient,
                registry,
                promptResource);
    }

    private void mockChatResponse(
            SkillSelectionResponse response) {

        when(
                chatClient.prompt())
                .thenReturn(
                        requestSpec);

        when(
                requestSpec.user(
                        any(
                                Consumer.class)))
                .thenReturn(
                        requestSpec);

        when(
                requestSpec.call())
                .thenReturn(
                        callResponseSpec);

        when(
                callResponseSpec.entity(
                        SkillSelectionResponse.class))
                .thenReturn(
                        response);
    }
}