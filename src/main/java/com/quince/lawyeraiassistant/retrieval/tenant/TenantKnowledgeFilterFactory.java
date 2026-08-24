package com.quince.lawyeraiassistant.retrieval.tenant;

public interface TenantKnowledgeFilterFactory {

    String createForTenant(
            String tenantId);

    String createSharedOnly();
}
