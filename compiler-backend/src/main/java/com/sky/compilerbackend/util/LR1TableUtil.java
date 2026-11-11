package com.sky.compilerbackend.util;

import com.sky.compilerbackend.exception.SyntaxException;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class LR1TableUtil {
    public static final Map<Integer, Map<String, String>> ACTION = new HashMap<>();
    public static final Map<Integer, Map<String, Integer>> GOTO = new HashMap<>();

    static {
        for (int i = 0; i <= 34; i++) {
            ACTION.put(i, new HashMap<>());
            GOTO.put(i, new HashMap<>());
        }

        // 状态 0-34 的全量映射 (基于之前的 LR(1) 计算)
        s(0, "if", 2); g(0, "S'", 1);
        a(1, "#", "accept");
        s(2, "i", 6); s(2, "(", 7); g(2, "A", 3); g(2, "B", 4); g(2, "F", 5);
        s(3, "then", 8);
        s(4, "!=", 13); s(4, ">", 14); s(4, "+", 18); r(4, "then", 5);
        r(5, "then", 7); r(5, "!=", 7); r(5, ">", 7); r(5, "+", 7);
        r(6, "then", 8); r(6, "!=", 8); r(6, ">", 8); r(6, "+", 8);
        s(7, "i", 21); s(7, "(", 7); g(7, "B", 19); g(7, "F", 20);
        s(8, "i", 10); g(8, "S", 9);
        r(9, "#", 1);
        s(10, ":=", 11);
        s(11, "i", 16); s(11, "(", 17); g(11, "B", 12); g(11, "F", 15);
        s(12, "+", 22); r(12, "#", 2);
        s(13, "i", 25); s(13, "(", 7); g(13, "B", 23); g(13, "F", 24);
        s(14, "i", 25); s(14, "(", 7); g(14, "B", 26); g(14, "F", 24);
        r(15, "+", 7); r(15, "#", 7);
        r(16, "+", 8); r(16, "#", 8);
        s(17, "i", 21); s(17, "(", 17); g(17, "B", 19); g(17, "F", 20);
        s(18, "i", 6); s(18, "(", 7); g(18, "F", 27);
        s(19, "+", 29); s(19, ")", 28);
        r(20, "+", 7); r(20, ")", 7);
        r(21, "+", 8); r(21, ")", 8);
        s(22, "i", 16); s(22, "(", 17); g(22, "F", 30);
        s(23, "+", 31); r(23, "then", 3);
        r(24, "then", 7); r(24, "+", 7);
        r(25, "then", 8); r(25, "+", 8);
        s(26, "+", 32); r(26, "then", 4);
        r(27, "then", 6); r(27, "!=", 6); r(27, ">", 6); r(27, "+", 6);
        r(28, "then", 9); r(28, "!=", 9); r(28, ">", 9); r(28, "+", 9);
        s(29, "i", 21); s(29, "(", 7); g(29, "F", 33);
        r(30, "+", 6); r(30, "#", 6);
        s(31, "i", 25); s(31, "(", 7); g(31, "F", 34);
        s(32, "i", 25); s(32, "(", 7); g(32, "F", 34);
        r(33, "+", 6); r(33, ")", 6);
        r(34, "then", 6); r(34, "+", 6);

        /**
         * additional
         */
        r(28, ")", 9); r(28, "#", 9);
    }

    private static void s(int s, String t, int ns) { ACTION.get(s).put(t, "shift " + ns); }
    private static void r(int s, String t, int p) { ACTION.get(s).put(t, "reduce " + p); }
    private static void a(int s, String t, String act) { ACTION.get(s).put(t, act); }
    private static void g(int s, String nt, int ns) { GOTO.get(s).put(nt, ns); }

    public String getAction(int state, String terminal) {
        String res = ACTION.get(state).get(terminal);
        if (res == null) throw new SyntaxException("状态 " + state + " 下无法识别符号 '" + terminal + "'");
        return res;
    }

    public int getGoto(int state, String nonTerminal) {
        Integer res = GOTO.get(state).get(nonTerminal);
        if (res == null) throw new SyntaxException("Goto表缺失：状态 " + state + " 缺少非终结符 " + nonTerminal);
        return res;
    }
}