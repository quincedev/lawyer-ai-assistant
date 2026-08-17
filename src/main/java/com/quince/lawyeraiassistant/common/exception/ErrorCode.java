package com.quince.lawyeraiassistant.common.exception;

public enum ErrorCode {

    VALIDATION_ERROR,

    MISSING_REQUEST_PARAMETER,

    INVALID_ARGUMENT,

    /*
     * =====================================================
     * AI / Agent Security
     * =====================================================
     */

    AI_INPUT_REJECTED,

    AI_OUTPUT_REJECTED,

    /*
     * 旧 SensitiveWordException 暂时继续使用。
     */
    AI_CONTENT_REJECTED,

    /*
     * =====================================================
     * Knowledge Base
     * =====================================================
     */

    KNOWLEDGE_BASE_INITIALIZATION_ERROR,

    KNOWLEDGE_BASE_DOCUMENT_LOAD_ERROR,

    KNOWLEDGE_BASE_VECTOR_WRITE_ERROR,

    KNOWLEDGE_BASE_VECTOR_SEARCH_ERROR,

    /*
     * =====================================================
     * Internal
     * =====================================================
     */

    INTERNAL_SERVER_ERROR
}