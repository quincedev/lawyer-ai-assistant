package com.quince.lawyeraiassistant.agent.config;

import com.quince.lawyeraiassistant.advisor.LoggingAdvisorV2;
import com.quince.lawyeraiassistant.agent.skill.AgentSkill;
import com.quince.lawyeraiassistant.agent.skill.AgentSkillRegistry;
import com.quince.lawyeraiassistant.agent.skill.selector.AgentSkillSelector;
import com.quince.lawyeraiassistant.agent.skill.selector.SpringAiAgentSkillSelector;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Set;

@Configuration
public class AgentSkillConfiguration {

    @Bean
    public AgentSkill legalResearchSkill() {

        return AgentSkill.of(
                "legal-research",
                "Legal Research",
                """
                        用于研究具体法律问题，
                        检索可靠法律依据，
                        并基于检索结果形成结构化法律分析。
                        """,
                """
                        你正在执行法律研究任务。

                        执行过程中应遵循以下原则：

                        1. 首先识别用户问题中的核心法律争议点。

                        2. 对需要法律依据支持的判断，
                           应优先检索法律知识库，
                           不应仅依赖模型自身记忆。

                        3. 区分：
                           - 已经检索确认的法律依据；
                           - 基于法律依据作出的分析和推理。

                        4. 如果已有检索结果不足以支持结论，
                           应补充检索或明确说明信息不足，
                           不得编造法律条文、司法解释或案例。

                        5. 分析时应关注：
                           - 法律规则；
                           - 构成要件；
                           - 法律后果；
                           - 适用条件；
                           - 必要的风险提示。

                        6. 最终结论应结构清晰，
                           并明确说明结论所依据的法律信息。
                        """,
                List.of(
                        "searchLegalKnowledge"),
                Set.of(
                        "legal",
                        "research",
                        "law"));
    }

    @Bean
    public AgentSkillRegistry agentSkillRegistry(
            List<AgentSkill> skills) {

        return new AgentSkillRegistry(
                skills);
    }

    @Bean("agentSkillChatClient")
    public ChatClient agentSkillChatClient(
            ChatClient.Builder builder,
            LoggingAdvisorV2 loggingAdvisorV2) {

        return builder
                .defaultAdvisors(
                        loggingAdvisorV2)
                .build();
    }

    @Bean
    public AgentSkillSelector agentSkillSelector(
            @org.springframework.beans.factory.annotation.Qualifier("agentSkillChatClient") ChatClient chatClient,
            AgentSkillRegistry skillRegistry,
            @Value("classpath:/prompts/agent/skill-selection.st") Resource promptResource) {

        return new SpringAiAgentSkillSelector(
                chatClient,
                skillRegistry,
                promptResource);
    }
}