package com.sky.compilerbackend.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 识别一个Token,DFA是正则表达式底层实现
 *
 * 当前状态	输入字符	下一状态
 * START-字母-IN_IDENTIFIER: token以字母开头
 * IN_IDENTIFIER-字母 / 数字-IN_IDENTIFIER： token中间是字母/数字
 * 任何状态-其他字符-ACCEPT： 遇到非字母/数字，token结束
 */
public class DFAUtil {
    // 状态枚举
    public enum State {
        START, IN_IDENTIFIER, IN_NUMBER, IN_OPERATOR, ACCEPT
    }

    // 状态转移表: Current State + Input Character -> Next State
    private static final Map<State, Map<Character, State>> transitionTable = new HashMap<>();

    static {
        // 初始化状态转移表
        transitionTable.put(State.START, new HashMap<>());
        transitionTable.put(State.IN_IDENTIFIER, new HashMap<>());
        transitionTable.put(State.IN_NUMBER, new HashMap<>());
        transitionTable.put(State.IN_OPERATOR, new HashMap<>());

        // START -> 字母 -> IN_IDENTIFIER
        for (char c = 'a'; c <= 'z'; c++) {
            transitionTable.get(State.START).put(c, State.IN_IDENTIFIER);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            transitionTable.get(State.START).put(c, State.IN_IDENTIFIER);
        }

        // START -> 数字 -> IN_NUMBER
        for (char c = '0'; c <= '9'; c++) {
            transitionTable.get(State.START).put(c, State.IN_NUMBER);
        }

        // START -> 运算符 -> IN_OPERATOR
        for (char c : "+>!=:".toCharArray()) {
            transitionTable.get(State.START).put(c, State.IN_OPERATOR);
        }

        // IN_IDENTIFIER -> 字母/数字 -> IN_IDENTIFIER
        for (char c = 'a'; c <= 'z'; c++) {
            transitionTable.get(State.IN_IDENTIFIER).put(c, State.IN_IDENTIFIER);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            transitionTable.get(State.IN_IDENTIFIER).put(c, State.IN_IDENTIFIER);
        }
        for (char c = '0'; c <= '9'; c++) {
            transitionTable.get(State.IN_IDENTIFIER).put(c, State.IN_IDENTIFIER);
        }

        // IN_NUMBER -> 数字 -> IN_NUMBER
        for (char c = '0'; c <= '9'; c++) {
            transitionTable.get(State.IN_NUMBER).put(c, State.IN_NUMBER);
        }

        // IN_OPERATOR -> 运算符 -> IN_OPERATOR
        for (char c : "+>!=:".toCharArray()) {
            transitionTable.get(State.IN_OPERATOR).put(c, State.IN_OPERATOR);
        }
    }

    // 获取下一个状态
    public static State getNextState(State current, char c) {
        return transitionTable.get(current).getOrDefault(c, State.ACCEPT);
    }
}