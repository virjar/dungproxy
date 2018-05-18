package com.virjar.dungproxy.newserver.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Created by virjar on 2018/5/18.
 * <br>统一json结构,这段代码后来再统一抽取吧
 */
@Data
public class WebJsonResponse<T> {

    private int status;
    private  String message;
    private  T data;

    @JsonCreator
    public WebJsonResponse(@JsonProperty("status") int status,
                           @JsonProperty("message") String message,
                           @JsonProperty("data") T data) {

        this.status = status;
        this.message = message;
        this.data = data;
    }
}
