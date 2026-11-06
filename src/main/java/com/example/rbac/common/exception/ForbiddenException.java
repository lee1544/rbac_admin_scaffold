package com.example.rbac.common.exception;

import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {

    private final Integer code;

    public ForbiddenException(String message) {
        super(message);
        this.code = 403;
    }
}
