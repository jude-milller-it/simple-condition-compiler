package com.sky.compilerbackend.pojo.vo;

import com.sky.compilerbackend.pojo.Token;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 词法分析结果
 */
@AllArgsConstructor
@Data
public class LexerResult {
    List<Token> tokens;
    Map<Integer, Map<String, String>> actionTable;
    Map<Integer, Map<String, Integer>> gotoTable;
}
