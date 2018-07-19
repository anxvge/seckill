package com.jas.seckill.model;

import java.io.Serializable;

/**
 * ClassName:ResponseMessage
 * Package:com.jas.seckill.model
 * Descrip:
 *
 * @Date:2018/7/17 下午7:56
 * @Author:jas
 */
public class ResponseMessage implements Serializable {

    private Integer ErrorCode;
    private String ErrorMessage;
    private Object ResponseObj;

    public Object getResponseObj() {
        return ResponseObj;
    }

    public void setResponseObj(Object responseObj) {
        ResponseObj = responseObj;
    }

    public Integer getErrorCode() {
        return ErrorCode;
    }

    public void setErrorCode(Integer errorCode) {
        ErrorCode = errorCode;
    }

    public String getErrorMessage() {
        return ErrorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        ErrorMessage = errorMessage;
    }
}
