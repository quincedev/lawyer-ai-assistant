package com.quince.lawyeraiassistant.security.tenant.quota;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.quince.lawyeraiassistant.security.tenant.TenantContext;

@Service
public final class InMemoryTenantResourceQuotaService
        implements TenantResourceQuotaService {

    private final ConcurrentHashMap<String, AtomicInteger> activeAgentExecutions = new ConcurrentHashMap<>();

    private final TenantResourceQuotaProperties properties;

    public InMemoryTenantResourceQuotaService(
            TenantResourceQuotaProperties properties) {

        this.properties = Objects.requireNonNull(
                properties,
                "properties must not be null");
    }

    @Override
    public TenantQuotaLease acquireAgentExecution(
            TenantContext tenantContext) {

        Objects.requireNonNull(
                tenantContext,
                "tenantContext must not be null");

        String tenantId = tenantContext.tenantId();

        AtomicInteger counter = activeAgentExecutions.computeIfAbsent(
                tenantId,
                ignored -> new AtomicInteger());

        int limit = properties
                .getMaxConcurrentAgentExecutions();

        while (true) {

            int current = counter.get();

            if (current >= limit) {

                cleanupIfUnused(
                        tenantId,
                        counter);

                throw new TenantResourceQuotaExceededException();
            }

            if (counter.compareAndSet(
                    current,
                    current + 1)) {

                return new DefaultTenantQuotaLease(
                        tenantId,
                        counter);
            }
        }
    }

    @Override
    public int activeAgentExecutions(
            String tenantId) {

        Objects.requireNonNull(
                tenantId,
                "tenantId must not be null");

        String normalized = tenantId.trim();

        if (normalized.isBlank()) {

            throw new IllegalArgumentException(
                    "tenantId must not be blank");
        }

        AtomicInteger counter = activeAgentExecutions.get(
                normalized);

        return counter == null
                ? 0
                : counter.get();
    }

    private void release(
            String tenantId,
            AtomicInteger counter) {

        int remaining = counter.decrementAndGet();

        if (remaining < 0) {

            counter.incrementAndGet();

            throw new IllegalStateException(
                    "Tenant Agent execution quota counter became negative");
        }

        cleanupIfUnused(
                tenantId,
                counter);
    }

    private void cleanupIfUnused(
            String tenantId,
            AtomicInteger counter) {

        if (counter.get() == 0) {

            activeAgentExecutions.remove(
                    tenantId,
                    counter);
        }
    }

    private final class DefaultTenantQuotaLease
            implements TenantQuotaLease {

        private final String tenantId;

        private final AtomicInteger counter;

        private final AtomicBoolean closed = new AtomicBoolean();

        private DefaultTenantQuotaLease(
                String tenantId,
                AtomicInteger counter) {

            this.tenantId = tenantId;

            this.counter = counter;
        }

        @Override
        public void close() {

            if (closed.compareAndSet(
                    false,
                    true)) {

                release(
                        tenantId,
                        counter);
            }
        }
    }
}