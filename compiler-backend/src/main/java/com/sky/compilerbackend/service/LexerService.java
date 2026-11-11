package com.sky.compilerbackend.service;

import com.sky.compilerbackend.pojo.vo.LexerResult;

/**
 * 词法分析器：识别关键字、标识符（变量名 函数名）、运算符、数字、界符
 */
public interface LexerService{
    LexerResult analyze(String sourceCode);
}