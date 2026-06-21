package com.zw.auth.handler;

import com.zw.common.constant.HttpStatus;
import com.zw.common.entity.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.fail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }
}
