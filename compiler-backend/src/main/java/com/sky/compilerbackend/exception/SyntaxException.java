package com.sky.compilerbackend.exception;

/**
 * 语法异常
 */
public class SyntaxException extends RuntimeException {
    public SyntaxException(String message) {
        super(message);
    }
}
