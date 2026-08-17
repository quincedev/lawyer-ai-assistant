package com.quince.lawyeraiassistant.security.legal;

import com.quince.lawyeraiassistant.security.SecurityTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SecurityTest
class SecuritySignalTest {

    @Test
    void shouldCreateSignal() {

        SecuritySignal signal = SecuritySignal.of(
                SecuritySignalType.PROMPT_INJECTION,
                SecuritySource.USER_INPUT,
                "override instruction detected");

        assertEquals(
                SecuritySignalType.PROMPT_INJECTION,
                signal.type());

        assertEquals(
                SecuritySource.USER_INPUT,
                signal.source());

        assertEquals(
                "override instruction detected",
                signal.detail());
    }

    @Test
    void shouldNormalizeNullDetail() {

        SecuritySignal signal = SecuritySignal.of(
                SecuritySignalType.RUNTIME_LIMIT_REACHED,
                SecuritySource.RUNTIME,
                null);

        assertEquals(
                "",
                signal.detail());
    }

    @Test
    void shouldRejectNullType() {

        assertThrows(
                NullPointerException.class,
                () -> SecuritySignal.of(
                        null,
                        SecuritySource.USER_INPUT,
                        ""));
    }
}
