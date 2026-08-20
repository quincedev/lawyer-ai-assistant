package com.quince.lawyeraiassistant.agent.action.policy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.agent.model.AgentContext;
import com.quince.lawyeraiassistant.agent.model.AgentTask;
import com.quince.lawyeraiassistant.agent.model.ToolObservation;

/**
 * 检测 RETRY 是否已经进入无进展循环。
 *
 * <p>
 * 典型场景：
 * </p>
 *
 * <pre>
 * Tool success
 * → Reflection RETRY
 * → same Tool
 * → same Evidence
 * → Reflection RETRY
 * </pre>
 *
 * 如果同一个 Task、同一个 Tool 已经连续拿到完全相同的成功 Evidence，
 * 再次执行相同 Tool 不可能产生新的信息，则认为 retry 已无进展。
 */
@Component
public class NoProgressRetryPolicy {

    /**
     * 至少出现两份完全相同的成功 Observation，
     * 才判定为 no-progress。
     *
     * 第一份 Evidence 本身不能直接判无进展，
     * 因为 Reflection 第一次 RETRY 仍可能通过不同参数获得新 Evidence。
     */
    private static final int REQUIRED_IDENTICAL_OBSERVATIONS = 2;

    public boolean isNoProgress(
            AgentContext context,
            AgentTask task) {

        Objects.requireNonNull(
                context,
                "AgentContext must not be null");

        Objects.requireNonNull(
                task,
                "AgentTask must not be null");

        List<ToolObservation> observations = context.getObservations()
                .stream()
                .filter(
                        observation -> task.getId()
                                .equals(
                                        observation.getTaskId()))
                .filter(
                        ToolObservation::isSuccess)
                .toList();

        if (observations.size() < REQUIRED_IDENTICAL_OBSERVATIONS) {

            return false;
        }

        ToolObservation latest = observations.get(
                observations.size() - 1);

        String latestFingerprint = fingerprint(
                latest);

        long identicalCount = observations.stream()
                .filter(
                        observation -> latest.getToolName()
                                .equals(
                                        observation.getToolName()))
                .map(
                        this::fingerprint)
                .filter(
                        latestFingerprint::equals)
                .count();

        return identicalCount >= REQUIRED_IDENTICAL_OBSERVATIONS;
    }

    public String fingerprint(
            ToolObservation observation) {

        Objects.requireNonNull(
                observation,
                "ToolObservation must not be null");

        String payload = observation.getToolName()
                + "\n"
                + normalize(
                        observation.getContent());

        try {

            MessageDigest digest = MessageDigest.getInstance(
                    "SHA-256");

            byte[] hash = digest.digest(
                    payload.getBytes(
                            StandardCharsets.UTF_8));

            return HexFormat.of()
                    .formatHex(
                            hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception);
        }
    }

    private String normalize(
            String content) {

        if (content == null) {

            return "";
        }

        return content
                .replace(
                        "\r\n",
                        "\n")
                .replace(
                        '\r',
                        '\n')
                .trim();
    }
}