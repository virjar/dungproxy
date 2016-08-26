package com.virjar.core.utils;

import java.io.IOException;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.virjar.core.exception.BusinessException;
import com.virjar.core.exception.RestApiError;
import com.virjar.core.rest.ResponseEnvelope;

public class ReturnUtil {

    private ReturnUtil() {
        super();
    }

    public static ResponseEntity<ResponseEnvelope<Object>> retFromReturnInfo(ReturnInfo retInfo) {
        if (retInfo.getStatusCode().equals(ReturnInfo.SUCCEED)) {
            return ReturnUtil.retSuccess(retInfo.getMessage());
        } else {
            if (StringUtils.isEmpty(retInfo.getThirdpartyStatusCode())) {
                return ReturnUtil.retException(retInfo.getStatusCode(), retInfo.getMessage());
            } else {
                return ReturnUtil.retException(retInfo.getStatusCode(), retInfo.getThirdpartyStatusCode() + " "
                        + retInfo.getMessage());
            }
        }
    }

    public static ResponseEntity<ResponseEnvelope<Object>> retError(ReturnCode rc, String rawMsg) {
        RestApiError restApiError = new RestApiError();
        restApiError.setStatusCode(rc.getCode());
        restApiError.setMessage(rc.getKey());
        restApiError.setRawMessage(rawMsg);
        ResponseEnvelope<Object> responseEnv = new ResponseEnvelope<Object>(rc, false);
        return new ResponseEntity<ResponseEnvelope<Object>>(responseEnv, HttpStatus.OK);
    }

    public static ResponseEntity<ResponseEnvelope<Object>> retSuccess(Object object) {
        ResponseEnvelope<Object> responseEnv = new ResponseEnvelope<Object>(object, true);
        return new ResponseEntity<ResponseEnvelope<Object>>(responseEnv, HttpStatus.OK);
    }

    public static ResponseEntity<ResponseEnvelope<Object>> retException(ReturnCode rc) {
        throw new BusinessException(rc.getKey(), rc.getCode());
    }

    public static ResponseEntity<ResponseEnvelope<Object>> retException(ReturnCode rc, String message) {
        throw new BusinessException(message, rc.getCode());
    }

    public static ResponseEntity<ResponseEnvelope<Object>> retException(String code, String message) {
        throw new BusinessException(message, code);
    }

    public static HttpServletResponse retErrorDetail(ReturnCode rc, ServletResponse response) throws IOException {
        HttpServletResponse servletResponse = (HttpServletResponse) response;
        servletResponse.setCharacterEncoding("UTF-8");
        servletResponse.setHeader("Content-type", "text/plain;charset=UTF-8");
        servletResponse.setStatus(HttpStatus.OK.value());
        RestApiError restApiError = new RestApiError();
        restApiError.setStatusCode(rc.getCode());
        restApiError.setMessage(rc.getKey());
        ResponseEnvelope<Object> envelope = new ResponseEnvelope<Object>(restApiError, false);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, Boolean.FALSE);
        String json = mapper.writeValueAsString(envelope);
        response.getWriter().write(json);
        return (HttpServletResponse) response;
    }
}