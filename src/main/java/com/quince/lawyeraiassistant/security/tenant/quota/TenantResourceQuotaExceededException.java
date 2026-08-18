package com.quince.lawyeraiassistant.security.tenant.quota;

import org.springframework.http.HttpStatus;

import com.quince.lawyeraiassistant.common.exception.BusinessException;
import com.quince.lawyeraiassistant.common.exception.ErrorCode;

public class TenantResourceQuotaExceededException
        extends BusinessException {

    private static final String SAFE_MESSAGE = "当前租户 AI 服务并发请求已达到上限，请稍后重试";

    public TenantResourceQuotaExceededException() {

        super(
                ErrorCode.TENANT_RESOURCE_QUOTA_EXCEEDED,
                SAFE_MESSAGE,
                HttpStatus.TOO_MANY_REQUESTS);
    }
}