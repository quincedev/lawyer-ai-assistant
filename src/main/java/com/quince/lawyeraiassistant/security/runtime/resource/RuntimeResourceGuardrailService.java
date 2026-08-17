package com.quince.lawyeraiassistant.security.runtime.resource;

public interface RuntimeResourceGuardrailService {

    RuntimeResourceResult evaluate(
            RuntimeResourceType resourceType,
            int resourceLength);
}