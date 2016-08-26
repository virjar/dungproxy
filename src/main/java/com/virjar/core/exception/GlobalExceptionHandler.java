package com.virjar.core.exception;

import com.virjar.core.rest.ResponseEnvelope;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

/** 
* Created by virjar on 16/5/11.最低优先级的异常处理器，将所有异常封装为JSON
*/
public class GlobalExceptionHandler implements HandlerExceptionResolver {
    private static final MappingJackson2JsonView JSON_VIEW = new MappingJackson2JsonView();

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        return buildErrorJson(ex);
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
        String localizedMsg=localizedMsg=e.getLocalizedMessage();
        restApiError.setMessage(localizedMsg);
        restApiError.setRawMessage(e.getMessage());
        ResponseEnvelope<Object> envelope=new ResponseEnvelope<Object>(restApiError,false);
        return envelope;
    }
}