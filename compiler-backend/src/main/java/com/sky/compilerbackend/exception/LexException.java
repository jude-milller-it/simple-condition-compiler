package com.sky.compilerbackend.exception;

/**
 * 词法分析异常
 */
public class LexException extends RuntimeException {
    public LexException(String message) {
        super(message);
    }
}
