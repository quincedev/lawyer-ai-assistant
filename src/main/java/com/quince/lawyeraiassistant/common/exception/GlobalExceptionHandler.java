package com.quince.lawyeraiassistant.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.quince.lawyeraiassistant.exception.SensitiveWordException;

import java.time.Instant;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request) {

        log.warn(
                "Authentication failed. path={}",
                request.getRequestURI());

        ErrorResponse response = createResponse(
                ErrorCode.AUTHENTICATION_FAILED,
                "Invalid username or password",
                HttpStatus.UNAUTHORIZED,
                request);

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request) {

        log.warn(
                "Business exception. code={}, path={}, message={}",
                exception.getCode(),
                request.getRequestURI(),
                exception.getMessage());

        ErrorResponse response = createResponse(
                exception.getErrorCode(),
                exception.getMessage(),
                exception.getStatus(),
                request);

        return ResponseEntity
                .status(exception.getStatus())
                .body(response);
    }

    @ExceptionHandler(SensitiveWordException.class)
    public ResponseEntity<ErrorResponse> handleSensitiveWordException(
            SensitiveWordException exception,
            HttpServletRequest request) {

        log.warn(
                "AI request rejected by content guardrail. path={}, matchedWord={}",
                request.getRequestURI(),
                exception.getMatchedWord());

        ErrorResponse response = createResponse(
                ErrorCode.AI_CONTENT_REJECTED,
                exception.getMessage(),
                HttpStatus.BAD_REQUEST,
                request);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 处理直接标注在 Controller 方法参数上的约束，
     * 例如 @RequestParam @NotBlank String question。
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request) {

        String message = exception.getAllErrors()
                .stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("请求参数校验失败");

        return buildBadRequest(
                ErrorCode.VALIDATION_ERROR,
                message,
                request);
    }

    /**
     * 处理 @RequestBody + @Valid 等对象参数校验。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String message = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("请求参数校验失败");

        return buildBadRequest(
                ErrorCode.VALIDATION_ERROR,
                message,
                request);
    }

    /**
     * 兼容使用类级别 @Validated 时可能产生的异常。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {

        String message = exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("请求参数校验失败");

        return buildBadRequest(
                ErrorCode.VALIDATION_ERROR,
                message,
                request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {

        String message = "缺少必填参数：" + exception.getParameterName();

        return buildBadRequest(
                ErrorCode.MISSING_REQUEST_PARAMETER,
                message,
                request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {

        return buildBadRequest(
                ErrorCode.INVALID_ARGUMENT,
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {

        log.error(
                "Unexpected exception. path={}",
                request.getRequestURI(),
                exception);

        ErrorResponse response = createResponse(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "系统内部错误，请稍后重试",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private ResponseEntity<ErrorResponse> buildBadRequest(
            ErrorCode errorCode,
            String message,
            HttpServletRequest request) {

        ErrorResponse response = createResponse(
                errorCode,
                message,
                HttpStatus.BAD_REQUEST,
                request);

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    private ErrorResponse createResponse(
            ErrorCode errorCode,
            String message,
            HttpStatus status,
            HttpServletRequest request) {

        return new ErrorResponse(
                errorCode.name(),
                message,
                status.value(),
                request.getRequestURI(),
                Instant.now());
    }
}
