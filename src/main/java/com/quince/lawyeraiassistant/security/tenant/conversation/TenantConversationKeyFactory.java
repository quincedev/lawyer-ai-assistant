package com.quince.lawyeraiassistant.security.tenant.conversation;

import com.quince.lawyeraiassistant.security.tenant.TenantContext;

public interface TenantConversationKeyFactory {

    String create(
            TenantContext tenantContext,
            String conversationId);
}