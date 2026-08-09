package com.quince.lawyeraiassistant.advisor;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.core.Ordered;

//@Component
public class LoggingAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(LoggingAdvisor.class);

    /**
     * 在 before() 和 after() 之间共享开始时间时使用的 Context Key。
     *
     * Key 应避免与其他 Advisor 冲突，
     * 因此通常加上项目名或类名作为前缀。
     */
    private static final String START_TIME_KEY = LoggingAdvisor.class.getName() + ".startTime";

    @Override
    public ChatClientRequest before(
            ChatClientRequest request,
            AdvisorChain advisorChain) {

        long startTime = System.nanoTime();

        String userText = Optional.ofNullable(
                request.prompt().getUserMessage())
                .map(message -> message.getText())
                .orElse("");

        log.info(
                "AI request started, advisor={}, userText={}",
                getName(),
                abbreviate(userText, 500));

        /*
         * ChatClientRequest 是不可变对象。
         * 要向 Context 中添加数据，需要 mutate 后重新构建。
         */
        return request.mutate()
                .context(START_TIME_KEY, startTime)
                .build();
    }

    @Override
    public ChatClientResponse after(
            ChatClientResponse response,
            AdvisorChain advisorChain) {

        long endTime = System.nanoTime();

        long startTime = Optional.ofNullable(
                response.context().get(START_TIME_KEY))
                .filter(Long.class::isInstance)
                .map(Long.class::cast)
                .orElse(endTime);

        long durationMillis = (endTime - startTime) / 1_000_000;

        String answer = extractAnswer(response);

        log.info(
                "AI request completed, advisor={}, durationMs={}, answer={}",
                getName(),
                durationMillis,
                abbreviate(answer, 1000));

        return response;
    }

    @Override
    public int getOrder() {
        /*
         * 数字越小，优先级越高。
         *
         * LoggingAdvisor 放在链条外层，
         * 才能统计包括 Memory Advisor 和模型调用在内的总耗时。
         */
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    private String extractAnswer(ChatClientResponse response) {

        if (response == null
                || response.chatResponse() == null
                || response.chatResponse().getResult() == null
                || response.chatResponse()
                        .getResult()
                        .getOutput() == null) {

            return "";
        }

        return Optional.ofNullable(
                response.chatResponse()
                        .getResult()
                        .getOutput()
                        .getText())
                .orElse("");
    }

    private String abbreviate(String value, int maxLength) {

        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = value
                .replace("\r", " ")
                .replace("\n", " ")
                .strip();

        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, maxLength) + "...";
    }
}