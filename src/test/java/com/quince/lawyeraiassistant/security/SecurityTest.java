package com.quince.lawyeraiassistant.security;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test as part of the Agent Security Regression Suite.
 *
 * Usage:
 *
 * @SecurityTest
 *               class SomeSecurityTest {
 *               }
 *
 *               Maven:
 *
 *               mvn test -Psecurity
 */
@Target({
        ElementType.TYPE,
        ElementType.METHOD
})
@Retention(RetentionPolicy.RUNTIME)
@Tag("security")
public @interface SecurityTest {
}