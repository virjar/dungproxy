package com.virjar.core.exception;

import com.virjar.core.rest.ResponseEnvelope;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

/** 
* Created by weijia.deng on 16-4-5. 全局异常处理<br/> controller 外部异常处理，如400，415，找不到controller等，
*/
public class BadRequestExceptionHandler implements Ordered, HandlerExceptionResolver {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final MappingJackson2JsonView JSON_VIEW = new MappingJackson2JsonView();

    private List<Class<?>> filterExceptions = new ArrayList<>();

    /** 
    * 导致400的异常，默认处理不会显示字段不匹配的原因
    */
    public void GlobalExceptionHandler() {
        filterExceptions.add(TypeMismatchException.class);
        filterExceptions.add(HttpMessageNotReadableException.class);
        filterExceptions.add(MissingServletRequestParameterException.class);
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler, Exception ex) {
        if (!needHandler(ex)) {
            return null;
        }
        logger.error("exception caught!! reason:{}, e:",ex.getMessage(),ex);
        return buildErrorJson(ex);
    }

    private boolean needHandler(Exception ex) {
        for (  Class<?> clazz : filterExceptions) {
            if (clazz.isAssignableFrom(ex.getClass())) {
                return true;
            }
        }
        return false;
    }

    private ModelAndView buildErrorPage(Exception ex) {
        ModelAndView modelAndView=new ModelAndView("forward:/WEB-INF/views/error.jsp");
        modelAndView.addObject("message",ex.getMessage());
        return modelAndView;
    }

    private ModelAndView buildErrorJson(Exception ex) {
        ModelAndView modelAndView=new ModelAndView(JSON_VIEW);
        ResponseEnvelope<Object> responseEnvelope=buildResponseEntity(ex);
        modelAndView.addObject("error",responseEnvelope.getError());
        modelAndView.addObject("status",responseEnvelope.isStatus());
        modelAndView.addObject("data",responseEnvelope.getData());
        return modelAndView;
    }

    private ResponseEnvelope<Object> buildResponseEntity(Exception e) {
        RestApiError restApiError=new RestApiError();
        restApiError.setStatusCode("-1");
        String localizedMsg=e.getLocalizedMessage();
        restApiError.setMessage(localizedMsg);
        restApiError.setRawMessage(e.getMessage());
        ResponseEnvelope<Object> envelope=new ResponseEnvelope<Object>(restApiError,false);
        return envelope;
    }

    public int getOrder() {
        return 0;
    }
}