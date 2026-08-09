package com.quince.lawyeraiassistant.prompt.builder;

import com.quince.lawyeraiassistant.agent.prompt.model.FinalAnswerPromptContext;
import com.quince.lawyeraiassistant.agent.prompt.model.PlanningPromptContext;
import com.quince.lawyeraiassistant.agent.prompt.model.ReasonPromptContext;
import com.quince.lawyeraiassistant.agent.prompt.model.ReflectionPromptContext;
import com.quince.lawyeraiassistant.agent.prompt.model.ReplanningPromptContext;
import com.quince.lawyeraiassistant.agent.prompt.model.RuntimeReasonPromptContext;
import com.quince.lawyeraiassistant.prompt.factory.PromptFactory;
import com.quince.lawyeraiassistant.prompt.knowledge.KnowledgeFormatter;
import com.quince.lawyeraiassistant.prompt.model.PromptContext;
import com.quince.lawyeraiassistant.prompt.model.PromptFragment;
import com.quince.lawyeraiassistant.prompt.template.TemplateRenderer;
import com.quince.lawyeraiassistant.prompt.template.TemplateVariables;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 项目统一 Prompt 构建器。
 *
 * <p>
 * 负责将静态 PromptFragment 与动态上下文组装为
 * Spring AI Prompt。
 * </p>
 *
 * <p>
 * 当前支持：
 * </p>
 *
 * <ul>
 * <li>法律 RAG 问答 Prompt</li>
 * <li>Agent Reason Prompt</li>
 * </ul>
 *
 * <p>
 * 后续 Planning、Tool、Reflection 可以继续在本构建器中
 * 增加对应的构建入口，但每个入口应保持职责清晰。
 * </p>
 */
@Component
public class PromptBuilder {

        private final PromptFactory promptFactory;

        private final TemplateRenderer templateRenderer;

        private final KnowledgeFormatter knowledgeFormatter;

        public PromptBuilder(
                        PromptFactory promptFactory,
                        TemplateRenderer templateRenderer,
                        KnowledgeFormatter knowledgeFormatter) {

                this.promptFactory = Objects.requireNonNull(
                                promptFactory,
                                "PromptFactory must not be null");

                this.templateRenderer = Objects.requireNonNull(
                                templateRenderer,
                                "TemplateRenderer must not be null");

                this.knowledgeFormatter = Objects.requireNonNull(
                                knowledgeFormatter,
                                "KnowledgeFormatter must not be null");
        }

        /**
         * 构建法律 RAG 问答 Prompt。
         *
         * <p>
         * 原 LegalPromptBuilder.build(...) 的逻辑迁移到此方法。
         * </p>
         *
         * @param context 法律 Prompt 上下文
         * @return Spring AI Prompt
         */
        public Prompt buildLegal(
                        PromptContext context) {

                validateLegalContext(context);

                PromptFragment systemFragment = promptFactory.lawyerSystem();

                validateFragment(
                                systemFragment,
                                "Lawyer System PromptFragment");

                TemplateVariables variables = TemplateVariables.from(
                                context,
                                knowledgeFormatter);

                String renderedSystemPrompt = renderFragment(
                                systemFragment,
                                variables.toMap(),
                                "Rendered lawyer system prompt");

                List<Message> messages = new ArrayList<>();

                messages.add(
                                new SystemMessage(
                                                renderedSystemPrompt));

                messages.add(
                                new UserMessage(
                                                context.getQuestion()));

                return new Prompt(messages);
        }

        /**
         * 构建 Agent Reason Prompt。
         *
         * <p>
         * Reason 阶段的任务是理解 Goal，而不是直接回答用户。
         * 当前模板整体作为 SystemMessage 发送，不再额外创建
         * UserMessage，避免 Goal 被重复注入。
         * </p>
         *
         * @param context Reason Prompt 上下文
         * @return Spring AI Prompt
         */
        public Prompt buildReason(
                        ReasonPromptContext context) {

                Objects.requireNonNull(
                                context,
                                "ReasonPromptContext must not be null");

                PromptFragment reasonFragment = promptFactory.agentReason();

                validateFragment(
                                reasonFragment,
                                "Agent reason PromptFragment");

                String renderedReasonPrompt = renderFragment(
                                reasonFragment,
                                context.toVariables(),
                                "Rendered agent reason prompt");

                return new Prompt(
                                List.of(
                                                new SystemMessage(
                                                                renderedReasonPrompt)));
        }

        /**
         * 构建 Agent Planning Prompt。
         *
         * @param context Planning Prompt 上下文
         * @return Spring AI Prompt
         */
        public Prompt buildPlanning(
                        PlanningPromptContext context) {

                Objects.requireNonNull(
                                context,
                                "PlanningPromptContext must not be null");

                PromptFragment planningFragment = promptFactory.agentPlanning();

                validateFragment(
                                planningFragment,
                                "Agent planning PromptFragment");

                String renderedPlanningPrompt = renderFragment(
                                planningFragment,
                                context.toVariables(),
                                "Rendered agent planning prompt");

                return new Prompt(
                                List.of(
                                                new SystemMessage(
                                                                renderedPlanningPrompt)));
        }

        /**
         * 构建 Agent Final Answer Prompt。
         *
         * <p>
         * Final Answer 阶段负责基于 Agent 已完成的执行上下文，
         * 生成最终面向用户的自然语言回答。
         * </p>
         *
         * @param context Final Answer Prompt 上下文
         * @return Spring AI Prompt
         */
        public Prompt buildFinalAnswer(
                        FinalAnswerPromptContext context) {

                Objects.requireNonNull(
                                context,
                                "FinalAnswerPromptContext must not be null");

                PromptFragment finalAnswerFragment = promptFactory.agentFinalAnswer();

                validateFragment(
                                finalAnswerFragment,
                                "Agent final answer PromptFragment");

                String renderedFinalAnswerPrompt = renderFragment(
                                finalAnswerFragment,
                                context.toVariables(),
                                "Rendered agent final answer prompt");

                return new Prompt(
                                List.of(
                                                new SystemMessage(
                                                                renderedFinalAnswerPrompt)));
        }

        /**
         * 构建 Agent Reflection Prompt。
         *
         * @param context Reflection Prompt 上下文
         * @return Spring AI Prompt
         */
        public Prompt buildReflection(
                        ReflectionPromptContext context) {

                Objects.requireNonNull(
                                context,
                                "ReflectionPromptContext must not be null");

                PromptFragment reflectionFragment = promptFactory.agentReflection();

                validateFragment(
                                reflectionFragment,
                                "Agent reflection PromptFragment");

                String renderedReflectionPrompt = renderFragment(
                                reflectionFragment,
                                context.toVariables(),
                                "Rendered agent reflection prompt");

                return new Prompt(
                                List.of(
                                                new SystemMessage(
                                                                renderedReflectionPrompt)));
        }

        /**
         * 构建 Agent Replanning Prompt。
         *
         * @param context Replanning Prompt 上下文
         * @return Spring AI Prompt
         */
        public Prompt buildReplanning(
                        ReplanningPromptContext context) {

                Objects.requireNonNull(
                                context,
                                "ReplanningPromptContext must not be null");

                PromptFragment replanningFragment = promptFactory.agentReplanning();

                validateFragment(
                                replanningFragment,
                                "Agent replanning PromptFragment");

                String renderedReplanningPrompt = renderFragment(
                                replanningFragment,
                                context.toVariables(),
                                "Rendered agent replanning prompt");

                return new Prompt(
                                List.of(
                                                new SystemMessage(
                                                                renderedReplanningPrompt)));
        }

        /**
         * 构建 Agent Runtime Reason Prompt。
         *
         * @param context Runtime Reason Prompt 上下文
         * @return Spring AI Prompt
         */
        public Prompt buildRuntimeReason(
                        RuntimeReasonPromptContext context) {

                Objects.requireNonNull(
                                context,
                                "RuntimeReasonPromptContext must not be null");

                PromptFragment runtimeReasonFragment = promptFactory.agentRuntimeReason();

                validateFragment(
                                runtimeReasonFragment,
                                "Agent runtime reason PromptFragment");

                String renderedRuntimeReasonPrompt = renderFragment(
                                runtimeReasonFragment,
                                context.toVariables(),
                                "Rendered agent runtime reason prompt");

                return new Prompt(
                                List.of(
                                                new SystemMessage(
                                                                renderedRuntimeReasonPrompt)));
        }

        /**
         * 使用统一模板渲染器渲染 PromptFragment。
         */
        private String renderFragment(
                        PromptFragment fragment,
                        Map<String, Object> variables,
                        String renderedPromptName) {

                String renderedPrompt = templateRenderer.render(
                                fragment.getContent(),
                                variables);

                validateRenderedPrompt(
                                renderedPrompt,
                                renderedPromptName);

                return renderedPrompt;
        }

        private void validateLegalContext(
                        PromptContext context) {

                Objects.requireNonNull(
                                context,
                                "PromptContext must not be null");

                if (context.getQuestion() == null
                                || context.getQuestion().isBlank()) {

                        throw new IllegalArgumentException(
                                        "Prompt question must not be blank");
                }
        }

        private void validateFragment(
                        PromptFragment fragment,
                        String fragmentName) {

                Objects.requireNonNull(
                                fragment,
                                fragmentName + " must not be null");

                if (fragment.getContent() == null
                                || fragment.getContent().isBlank()) {

                        throw new IllegalArgumentException(
                                        fragmentName
                                                        + " content must not be blank");
                }
        }

        private void validateRenderedPrompt(
                        String renderedPrompt,
                        String renderedPromptName) {

                if (renderedPrompt == null
                                || renderedPrompt.isBlank()) {

                        throw new IllegalStateException(
                                        renderedPromptName
                                                        + " must not be blank");
                }
        }
}