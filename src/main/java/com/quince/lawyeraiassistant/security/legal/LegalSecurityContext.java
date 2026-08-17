package com.quince.lawyeraiassistant.security.legal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LegalSecurityContext {

    private final SecuritySource source;

    private final SecurityTrustLevel trustLevel;

    private final List<SecuritySignal> signals;

    private LegalSecurityContext(
            SecuritySource source,
            SecurityTrustLevel trustLevel,
            List<SecuritySignal> signals) {

        this.source = Objects.requireNonNull(
                source,
                "source must not be null");

        this.trustLevel = Objects.requireNonNull(
                trustLevel,
                "trustLevel must not be null");

        Objects.requireNonNull(
                signals,
                "signals must not be null");

        this.signals = List.copyOf(
                signals);
    }

    public static LegalSecurityContext of(
            SecuritySource source,
            SecurityTrustLevel trustLevel) {

        return new LegalSecurityContext(
                source,
                trustLevel,
                List.of());
    }

    public LegalSecurityContext withSignal(
            SecuritySignal signal) {

        Objects.requireNonNull(
                signal,
                "signal must not be null");

        List<SecuritySignal> updated = new ArrayList<>(
                signals);

        updated.add(
                signal);

        return new LegalSecurityContext(
                source,
                trustLevel,
                updated);
    }

    public LegalSecurityContext withSignals(
            List<SecuritySignal> additionalSignals) {

        Objects.requireNonNull(
                additionalSignals,
                "additionalSignals must not be null");

        List<SecuritySignal> updated = new ArrayList<>(
                signals);

        updated.addAll(
                additionalSignals);

        return new LegalSecurityContext(
                source,
                trustLevel,
                updated);
    }

    public boolean hasSignal(
            SecuritySignalType type) {

        Objects.requireNonNull(
                type,
                "type must not be null");

        return signals.stream()
                .anyMatch(
                        signal -> signal.type() == type);
    }

    public boolean isTrusted() {

        return trustLevel == SecurityTrustLevel.TRUSTED;
    }

    public boolean isUntrusted() {

        return trustLevel == SecurityTrustLevel.UNTRUSTED;
    }

    public SecuritySource source() {

        return source;
    }

    public SecurityTrustLevel trustLevel() {

        return trustLevel;
    }

    public List<SecuritySignal> signals() {

        return signals;
    }
}