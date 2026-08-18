package com.quince.lawyeraiassistant.rag.vector.tenant;

import org.springframework.ai.document.Document;

public interface TenantKnowledgeAccessPolicy {

    boolean canAccess(
            Document document,
            String tenantId);

    boolean canAccessSharedOnly(
            Document document);
}