package com.quince.lawyeraiassistant.prompt.factory;

import com.quince.lawyeraiassistant.prompt.definition.PromptDefinition;
import com.quince.lawyeraiassistant.prompt.model.PromptFragment;
import com.quince.lawyeraiassistant.prompt.registry.PromptRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * PromptFactory 的默认实现。
 *
 * <p>
 * 当前从内存中的 PromptRegistry 获取 Prompt。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class DefaultPromptFactory
                implements PromptFactory {

        private final PromptRegistry promptRegistry;

        /**
         * 根据逻辑名称获取 Prompt。
         */
        @Override
        public PromptFragment get(
                        String name) {

                validateName(name);

                return promptRegistry.find(name);
        }

        /**
         * 获取律师助手系统 Prompt。
         */
        @Override
        public PromptFragment lawyerSystem() {
                return get(
                                PromptDefinition.LAWYER_SYSTEM
                                                .getName());
        }

        /**
         * 获取 Agent Reason Prompt。
         */
        @Override
        public PromptFragment agentReason() {
                return get(
                                PromptDefinition.AGENT_REASON
                                                .getName());
        }

        /**
         * 获取 Agent Planning Prompt。
         */
        @Override
        public PromptFragment agentPlanning() {
                return get(
                                PromptDefinition.AGENT_PLANNING
                                                .getName());
        }

        private void validateName(
                        String name) {

                Objects.requireNonNull(
                                name,
                                "Prompt name must not be null");

                if (name.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Prompt name must not be blank");
                }
        }

        /**
         * 获取 Agent Final Answer Prompt。
         */
        @Override
        public PromptFragment agentFinalAnswer() {

                return get(
                                PromptDefinition.AGENT_FINAL_ANSWER
                                                .getName());
        }

        /**
         * 获取 Agent Reflection Prompt。
         */
        @Override
        public PromptFragment agentReflection() {

                return get(
                                PromptDefinition.AGENT_REFLECTION
                                                .getName());
        }

        /**
         * 获取 Agent Replanning Prompt。
         */
        @Override
        public PromptFragment agentReplanning() {

                return get(
                                PromptDefinition.AGENT_REPLANNING
                                                .getName());
        }

        @Override
        public PromptFragment agentRuntimeReason() {

                return get(
                                PromptDefinition.AGENT_RUNTIME_REASON
                                                .getName());
        }
}