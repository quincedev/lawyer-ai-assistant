package com.quince.lawyeraiassistant.rag.service;

import com.quince.lawyeraiassistant.prompt.builder.PromptBuilder;
import com.quince.lawyeraiassistant.prompt.model.PromptContext;
import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.retrieval.model.RetrieverContext;
import com.quince.lawyeraiassistant.retrieval.orchestration.RetrievalOrchestrator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 法律 RAG 问答服务。
 *
 * <p>
 * 正式调用链：
 * </p>
 *
 * <pre>
 * question
 *      ↓
 * RetrievalOrchestrator
 *      ↓
 * QueryPipeline
 *      ↓
 * RetrieverPipeline
 *      ↓
 * List&lt;Document&gt;
 *      ↓
 * PromptContext
 *      ↓
 * PromptBuilder.buildLegal()
 *      ↓
 * ragChatClient
 *      ↓
 * LLM
 * </pre>
 */
@Service
@ConditionalOnRagEnabled
public class RagChatService {

        private static final String DEFAULT_LEGAL_DOMAIN = "劳动法";

        private final ChatClient ragChatClient;

        private final RetrievalOrchestrator retrievalOrchestrator;

        private final PromptBuilder promptBuilder;

        public RagChatService(
                        @Qualifier("ragChatClient") ChatClient ragChatClient,
                        RetrievalOrchestrator retrievalOrchestrator,
                        PromptBuilder promptBuilder) {

                this.ragChatClient = Objects.requireNonNull(
                                ragChatClient,
                                "ragChatClient must not be null");

                this.retrievalOrchestrator = Objects.requireNonNull(
                                retrievalOrchestrator,
                                "retrievalOrchestrator must not be null");

                this.promptBuilder = Objects.requireNonNull(
                                promptBuilder,
                                "promptBuilder must not be null");
        }

        /**
         * 执行无会话编号的法律问答。
         */
        public String chat(
                        String question) {

                return chat(
                                question,
                                null);
        }

        /**
         * 执行完整法律 RAG 问答。
         */
        public String chat(
                        String question,
                        String conversationId) {

                validateQuestion(question);

                RetrieverContext retrievalContext = retrievalOrchestrator.retrieve(
                                question,
                                normalizeConversationId(
                                                conversationId));

                PromptContext promptContext = createPromptContext(
                                retrievalContext);

                Prompt prompt = promptBuilder.buildLegal(
                                promptContext);

                return ragChatClient
                                .prompt(prompt)
                                .call()
                                .content();
        }

        /**
         * 使用 Retriever Pipeline 的结果创建 PromptContext。
         */
        private PromptContext createPromptContext(
                        RetrieverContext retrievalContext) {

                Objects.requireNonNull(
                                retrievalContext,
                                "RetrieverContext must not be null");

                return PromptContext.builder()
                                .question(
                                                retrievalContext
                                                                .getQueryContext()
                                                                .getQuestion())
                                .conversationId(
                                                retrievalContext
                                                                .getQueryContext()
                                                                .getConversationId())
                                .variable(
                                                "legalDomain",
                                                DEFAULT_LEGAL_DOMAIN)
                                .knowledge(
                                                retrievalContext
                                                                .getDocuments())
                                .build();
        }

        private void validateQuestion(
                        String question) {

                Objects.requireNonNull(
                                question,
                                "Question must not be null");

                if (question.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Question must not be blank");
                }
        }

        private String normalizeConversationId(
                        String conversationId) {

                if (conversationId == null
                                || conversationId.isBlank()) {

                        return null;
                }

                return conversationId.trim();
        }
}