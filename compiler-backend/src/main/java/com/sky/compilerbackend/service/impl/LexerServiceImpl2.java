package com.sky.compilerbackend.service.impl;

import com.sky.compilerbackend.pojo.vo.LexerResult;
import com.sky.compilerbackend.pojo.Token;
import com.sky.compilerbackend.service.LexerService;
import com.sky.compilerbackend.util.DFAUtil;
import com.sky.compilerbackend.util.LR1TableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 词法分析器：DFA实现（每个字符只需要一次状态转移，时间复杂度为 O(n)）
 */
@Service
public class LexerServiceImpl2 implements LexerService {
    @Autowired
    private LR1TableUtil lr1TableUtil;
    private static final String[] KEYWORDS = {"if", "then"};
    private static final String[] OPERATORS = {"+", ">", "!=", ":="};
    private static final String[] DELIMITERS = {"(", ")", " "};

    public LexerResult analyze(String sourceCode) {
        List<Token> tokens = new ArrayList<>();
        String[] lines = sourceCode.split("\n");
        int lineNum = 1;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                lineNum++;
                continue;
            }

            int pos = 0; //position
            while (pos < line.length()) {
                char c = line.charAt(pos);
                if (Character.isWhitespace(c)) {
                    pos++;
                    continue;
                }

                // 用 DFA 扫描 token（每次一个token）
                int end = pos;
                DFAUtil.State current = DFAUtil.State.START;
                while (end < line.length()) {
                    char nextChar = line.charAt(end);
                    DFAUtil.State nextState = DFAUtil.getNextState(current, nextChar);
                    if (nextState == DFAUtil.State.ACCEPT) {
                        break;
                    }
                    current = nextState;
                    end++;
                }

                String tokenValue = line.substring(pos, end);
                String tokenType = classifyToken(tokenValue);
                tokens.add(new Token(tokenType, tokenValue, lineNum));
                pos = end;
            }
            lineNum++;
        }
        return new LexerResult(tokens, lr1TableUtil.ACTION, lr1TableUtil.GOTO);
    }

    private String classifyToken(String value) {
        if (isKeyword(value)) return "关键字";
        if (isOperator(value)) return "运算符";
        if (isDelimiter(value)) return "界符";
        if (value.matches("\\d+")) return "数字";
        return "标识符";
    }

    private boolean isKeyword(String word) {
        for (String kw : KEYWORDS) {
            if (kw.equals(word)) return true;
        }
        return false;
    }

    private boolean isOperator(String op) {
        for (String o : OPERATORS) {
            if (o.equals(op)) return true;
        }
        return false;
    }

    private boolean isDelimiter(String del) {
        for (String d : DELIMITERS) {
            if (d.equals(del)) return true;
        }
        return false;
    }
}