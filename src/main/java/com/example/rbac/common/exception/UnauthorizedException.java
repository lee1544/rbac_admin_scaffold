package com.example.rbac.common.exception;

import lombok.Getter;

@Getter
public class UnauthorizedException extends RuntimeException {

    private final Integer code;

    public UnauthorizedException(String message) {
        super(message);
        this.code = 401;
    }
}
