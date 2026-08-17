package com.quince.lawyeraiassistant.security.legal;

import java.util.Objects;

public record SecuritySignal(
        SecuritySignalType type,
        SecuritySource source,
        String detail) {

    public SecuritySignal {

        Objects.requireNonNull(
                type,
                "type must not be null");

        Objects.requireNonNull(
                source,
                "source must not be null");

        detail = detail == null
                ? ""
                : detail.trim();
    }

    public static SecuritySignal of(
            SecuritySignalType type,
            SecuritySource source,
            String detail) {

        return new SecuritySignal(
                type,
                source,
                detail);
    }
}