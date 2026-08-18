package com.quince.lawyeraiassistant.security.tenant.authorization;

import org.springframework.http.HttpStatus;

import com.quince.lawyeraiassistant.common.exception.BusinessException;
import com.quince.lawyeraiassistant.common.exception.ErrorCode;

public class TenantAccessDeniedException
        extends BusinessException {

    private static final String SAFE_MESSAGE = "当前用户无权访问该 AI 服务";

    public TenantAccessDeniedException() {

        super(
                ErrorCode.TENANT_ACCESS_DENIED,
                SAFE_MESSAGE,
                HttpStatus.FORBIDDEN);
    }
}