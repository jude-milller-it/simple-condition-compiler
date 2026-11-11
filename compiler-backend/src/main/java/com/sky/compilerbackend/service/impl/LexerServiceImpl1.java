package com.sky.compilerbackend.service.impl;

import com.sky.compilerbackend.pojo.vo.LexerResult;
import com.sky.compilerbackend.pojo.Token;
import com.sky.compilerbackend.exception.LexException;
import com.sky.compilerbackend.service.LexerService;
import com.sky.compilerbackend.util.LR1TableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 词法分析器：手动扫描实现
 */
@Service
public class LexerServiceImpl1 implements LexerService {
    @Autowired
    private LR1TableUtil lr1TableUtil;
    // 关键字
    private static final String[] KEYWORDS = {"if", "then"};
    // 运算符
    private static final String[] OPERATORS = {"+", ">", "!=", ":="};
    // 界符
    private static final String[] DELIMITERS = {"(", ")", " "};

    /**
     * 词法分析核心方法
     * @param sourceCode 源代码
     * @return 二元式序列
     */
    @Override
    public LexerResult analyze(String sourceCode) {
        List<Token> tokens = new ArrayList<>();
        // 按行分割，记录行号
        String[] lines = sourceCode.split("\n");
        int lineNum = 1;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                lineNum++;
                continue;
            }

            int pos = 0;    //position
            while (pos < line.length()) {
                char c = line.charAt(pos);

                // 跳过空格
                if (Character.isWhitespace(c)) {
                    pos++;
                    continue;
                }

                //以下判断c（第一个字符）
                // 匹配关键字/标识符
                if (Character.isLetter(c)) {
                    int end = pos;
                    while (end < line.length() && (Character.isLetterOrDigit(line.charAt(end)))) {
                        end++;
                    }
                    String word = line.substring(pos, end);
                    // 判断是否为关键字
                    if (isKeyword(word)) {
                        tokens.add(new Token("关键字", word, lineNum));
                    } else {
                        tokens.add(new Token("标识符", word, lineNum));
                    }
                    pos = end;
                    continue;
                }

                // 匹配数字
                if (Character.isDigit(c)) {
                    int end = pos;
                    while (end < line.length() && Character.isDigit(line.charAt(end))) {
                        end++;
                    }
                    String num = line.substring(pos, end);
                    tokens.add(new Token("数字", num, lineNum));
                    pos = end;
                    continue;
                }

                // 匹配多字符运算符（!=、:=）
                if (pos + 1 < line.length()) {
                    String twoCharOp = line.substring(pos, pos + 2);
                    if (isOperator(twoCharOp)) {
                        tokens.add(new Token("运算符", twoCharOp, lineNum));
                        pos += 2;
                        continue;
                    }
                }

                // 匹配单字符运算符/界符
                String singleChar = String.valueOf(c);
                if (isOperator(singleChar) || isDelimiter(singleChar)) {
                    String type = isOperator(singleChar) ? "运算符" : "界符";
                    tokens.add(new Token(type, singleChar, lineNum));
                    pos++;
                    continue;
                }

                // 非法字符
                throw new LexException("词法错误：第" + lineNum + "行存在非法字符 '" + c + "'");
            }
            lineNum++;
        }

        return new LexerResult(tokens, lr1TableUtil.ACTION, lr1TableUtil.GOTO);
    }

    /**
     * 判断是否为关键字
     */
    private boolean isKeyword(String word) {
        for (String kw : KEYWORDS) {
            if (kw.equals(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为运算符
     */
    private boolean isOperator(String op) {
        for (String o : OPERATORS) {
            if (o.equals(op)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为界符
     */
    private boolean isDelimiter(String del) {
        for (String d : DELIMITERS) {
            if (d.equals(del)) {
                return true;
            }
        }
        return false;
    }
}

