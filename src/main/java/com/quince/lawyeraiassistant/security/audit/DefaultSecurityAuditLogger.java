package com.quince.lawyeraiassistant.security.audit;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.quince.lawyeraiassistant.security.metrics.SecurityMetricsRecorder;

@Component
public final class DefaultSecurityAuditLogger
                implements SecurityAuditLogger {

        private static final Logger LOGGER = LoggerFactory.getLogger(
                        "SECURITY_AUDIT");

        private final SecurityMetricsRecorder securityMetricsRecorder;

        public DefaultSecurityAuditLogger(
                        SecurityMetricsRecorder securityMetricsRecorder) {

                this.securityMetricsRecorder = Objects.requireNonNull(
                                securityMetricsRecorder,
                                "securityMetricsRecorder must not be null");
        }

        @Override
        public void log(
                        SecurityAuditEvent event) {

                Objects.requireNonNull(
                                event,
                                "SecurityAuditEvent must not be null");

                recordLog(
                                event);

                securityMetricsRecorder.record(
                                event);
        }

        private void recordLog(
                        SecurityAuditEvent event) {

                String message = "securityEvent={} component={} reason={} metadata={}";

                switch (event.severity()) {

                        case INFO ->
                                LOGGER.info(
                                                message,
                                                event.type(),
                                                event.component(),
                                                event.reason(),
                                                event.metadata());

                        case WARN ->
                                LOGGER.warn(
                                                message,
                                                event.type(),
                                                event.component(),
                                                event.reason(),
                                                event.metadata());

                        case ERROR ->
                                LOGGER.error(
                                                message,
                                                event.type(),
                                                event.component(),
                                                event.reason(),
                                                event.metadata());
                }
        }
}