package com.virjar.core.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.virjar.core.rest.ResponseEnvelope;

/**
 * 对于controller发生的内部异常，在这里处理，他只会处理BusinessException和ServerInternalException
 * 其中ServerInternalException将会打印日志，同时构建json数据结构；BusinessException只会构建json数据结构
 */
@ControllerAdvice(annotations = Controller.class)
public class RestApiResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(RestApiResponseEntityExceptionHandler.class);

    @ExceptionHandler(value = { BusinessException.class, ServerInternalException.class })
    public ResponseEntity<Object> handleControllerException(Exception ex, WebRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.OK;
        if (ex instanceof BusinessException) {
            return handleBusinessException((BusinessException) ex, headers, status, request);
        }
        if (ex instanceof ServerInternalException) {
            logger.error("server internal Exception", ex);
            return handleServerInternalException((ServerInternalException) ex, headers, status, request);
        }
        throw ex;
    }

    protected ResponseEntity<Object> handleServerInternalException(ServerInternalException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        RestApiError restApiError = new RestApiError();
        restApiError.setStatusCode(ex.getErrorCode());
        restApiError.setMessage(ex.getLocalizedMessage());
        restApiError.setRawMessage(ex.getMessage());
        ResponseEnvelope<Object> envelope = new ResponseEnvelope<Object>(restApiError, false);
        return handleExceptionInternal(ex, envelope, headers, status, request);
    }

    protected ResponseEntity<Object> handleBusinessException(BusinessException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        RestApiError restApiError = new RestApiError();
        restApiError.setStatusCode(ex.getErrorCode());
        restApiError.setMessage(ex.getLocalizedMessage());
        restApiError.setRawMessage(ex.getMessage());
        ResponseEnvelope<Object> envelope = new ResponseEnvelope<Object>(restApiError, false);
        return handleExceptionInternal(ex, envelope, headers, status, request);
    }

    protected ResponseEntity<Object> handleBeanValidationException(BeanValidationException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        RestApiError restApiError = new RestApiError();
        restApiError.setStatusCode(ex.getErrorCode());
        ValidationError validationError = new ValidationError();
        Errors errors = ex.getErrors();
        if (errors.hasGlobalErrors()) {
            Map<String, String> globalErrors = new HashMap<String, String>();
            List<ObjectError> objectErrorList = errors.getGlobalErrors();
            for (ObjectError objectError : objectErrorList) {
                globalErrors.put(objectError.getObjectName(), objectError.getDefaultMessage());
            }
            validationError.setGlobalErrors(globalErrors);
        }
        if (errors.hasFieldErrors()) {
            Map<String, String> fieldErrors = new HashMap<String, String>();
            List<FieldError> fieldErroList = errors.getFieldErrors();
            for (FieldError fieldError : fieldErroList) {
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
            validationError.setFieldErrors(fieldErrors);
        }
        restApiError.setValidationError(validationError);
        ResponseEnvelope<Object> envelope = new ResponseEnvelope<Object>(restApiError, false);
        return handleExceptionInternal(ex, envelope, headers, status, request);
    }
}