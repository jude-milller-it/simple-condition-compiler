package com.sky.compilerbackend.exception;

/**
 * 语义异常
 */
public class SemanticException extends RuntimeException {
    public SemanticException(String message) {
        super(message);
    }
}
