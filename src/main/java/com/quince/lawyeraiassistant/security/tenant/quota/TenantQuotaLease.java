package com.quince.lawyeraiassistant.security.tenant.quota;

public interface TenantQuotaLease
        extends AutoCloseable {

    @Override
    void close();
}