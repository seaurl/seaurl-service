package com.seaurl.platform.users.exception;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(CustomExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResultInfo handlerException(Exception e) {
        log.error("unhandled exception: {}", e.getMessage(), e);
        return new ResultInfo(ResultStatus.Unknown_Exception);
    }
}
