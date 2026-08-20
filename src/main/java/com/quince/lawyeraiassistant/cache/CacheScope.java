package com.quince.lawyeraiassistant.cache;

/**
 * AI Cache 数据作用域。
 *
 * <p>
 * SHARED:
 * 可跨 Tenant 共享的公共知识缓存。
 * </p>
 *
 * <p>
 * TENANT:
 * Tenant 私有数据产生的缓存，
 * Cache Key 必须包含 tenantId。
 * </p>
 */
public enum CacheScope {

    SHARED,

    TENANT
}