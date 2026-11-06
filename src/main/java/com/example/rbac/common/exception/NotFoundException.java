package com.example.rbac.common.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {

    private final Integer code;

    public NotFoundException(String message) {
        super(message);
        this.code = 404;
    }
}
