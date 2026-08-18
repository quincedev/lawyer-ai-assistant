package com.quince.lawyeraiassistant.rag.vector.tenant;

public interface TenantKnowledgeFilterFactory {

    String createForTenant(
            String tenantId);

    String createSharedOnly();
}