package com.sky.compilerbackend.service;

import com.sky.compilerbackend.pojo.*;
import com.sky.compilerbackend.pojo.vo.AnalysisResult;
import com.sky.compilerbackend.exception.*;
import com.sky.compilerbackend.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ParserService {
    private final LR1TableUtil lr1TableUtil;
    private final SymbolTable symbolTable;
    private static final List<Production> PRODUCTIONS = new ArrayList<>();

    static class Production {
        String left;
        int length;
        String desc;
        public Production(String left, int length, String desc) {
            this.left = left; this.length = length; this.desc = desc;
        }
    }

    static {
        PRODUCTIONS.add(new Production("M", 1, "M -> S'"));
        PRODUCTIONS.add(new Production("S'", 4, "S' -> if A then S"));
        PRODUCTIONS.add(new Production("S", 3, "S -> i := B"));
        PRODUCTIONS.add(new Production("A", 3, "A -> B != B"));
        PRODUCTIONS.add(new Production("A", 3, "A -> B > B"));
        PRODUCTIONS.add(new Production("A", 1, "A -> B"));
        PRODUCTIONS.add(new Production("B", 3, "B -> B + F"));
        PRODUCTIONS.add(new Production("B", 1, "B -> F"));
        PRODUCTIONS.add(new Production("F", 1, "F -> i"));
        PRODUCTIONS.add(new Production("F", 3, "F -> ( B )"));
    }

    public ParserService(LR1TableUtil lr1TableUtil, SymbolTable symbolTable) {
        this.lr1TableUtil = lr1TableUtil;
        this.symbolTable = symbolTable;
    }

    public AnalysisResult analyze(List<Token> tokens) {
        StringBuilder logBuilder = new StringBuilder(); // 用于返回给前端的 message

        // 1. 定义表头：增加 NodeStack (符号节点栈) 的展示
        String header = String.format("%-25s | %-40s | %-10s | %-15s\n",
                "State Stack", "Node Stack (Symbols)", "Input", "Action");
        logBuilder.append(header).append("-".repeat(100)).append("\n");

        // 控制台同步输出
        System.out.println("\n>>>>>> 语法分析控制台日志启动 <<<<<<");
        System.out.print(header);
        System.out.println("-".repeat(100));

        symbolTable.clear();
        List<Token> input = new ArrayList<>(tokens);
        input.add(new Token("界符", "#",0));

        Stack<Integer> stateStack = new Stack<>();
        Stack<TreeNode> nodeStack = new Stack<>(); // 这个栈里存的是带真正的 Value 的节点

        stateStack.push(0);
        // nodeStack 起始为空，或压入一个标识符 #
        nodeStack.push(new TreeNode("#", "#"));

        int ptr = 0;
        try {
            while (ptr < input.size()) {
                Token token = input.get(ptr);
                String terminal = mapTokenToTerminal(token);
                int currentState = stateStack.peek();
                String action = lr1TableUtil.getAction(currentState, terminal);

                // 2. 准备 NodeStack 的可视化字符串 (只展示节点名，如果是 i 则展示具体的 i(a))
                String nodeStackStr = nodeStack.stream()
                        .map(n -> n.getValue() != null && !n.getValue().equals(n.getName())
                                ? n.getName() + "(" + n.getValue() + ")" : n.getName())
                        .collect(Collectors.joining(" "));

                // 3. 构建当前步骤行
                String logLine = String.format("%-25s | %-40s | %-10s | %-15s\n",
                        stateStack.toString(), nodeStackStr, token.getValue(), action);

                // 同步追加到两个地方
                logBuilder.append(logLine);
                System.out.print(logLine);

                // --- 逻辑执行 ---
                if (action.startsWith("shift")) {
                    int nextState = Integer.parseInt(action.split(" ")[1]);
                    stateStack.push(nextState);

                    // 真正存入符号的节点
                    nodeStack.push(new TreeNode(terminal, token.getValue()));

                    if (token.getType().equals("标识符")) {
                        symbolTable.addSymbol(token.getValue(), "int", "global");
                        // 符号表变更日志同步记录
                        String symLog = "  [SYMBOL] Add: " + token.getValue() + " to Global Table\n";
                        logBuilder.append(symLog);
                        System.out.print(symLog);
                    }
                    ptr++;
                }
                else if (action.startsWith("reduce")) {
                    int prodIdx = Integer.parseInt(action.split(" ")[1]);
                    Production prod = PRODUCTIONS.get(prodIdx);

                    String redLog = "  [REDUCE] Rule: " + prod.desc + "\n";
                    logBuilder.append(redLog);
                    System.out.print(redLog);

                    TreeNode parentNode = new TreeNode(prod.left);
                    for (int i = 0; i < prod.length; i++) {
                        stateStack.pop();
                        // 维持树的左右顺序
                        parentNode.getChildren().add(0, nodeStack.pop());
                    }

                    int gotoState = lr1TableUtil.getGoto(stateStack.peek(), prod.left);
                    stateStack.push(gotoState);
                    nodeStack.push(parentNode);
                }
                else if (action.equals("accept")) {
                    logBuilder.append("-------------------- 分析成功 --------------------\n");
                    System.out.println("-------------------- 分析成功 --------------------");

                    TreeNode root = nodeStack.pop();

                    // 最终树输出到控制台
                    System.out.println("\n--- FINAL AST VISUALIZATION ---");
                    renderTree(root, "", true);

                    return new AnalysisResult(root, new ArrayList<>(symbolTable.getAllSymbols()), logBuilder.toString(), symbolTable);
                }
            }
        } catch (Exception e) {
            String errorMsg = "\n[FATAL ERROR] " + e.getMessage();
            logBuilder.append(errorMsg);
            System.err.println(errorMsg);
            throw new SyntaxException(logBuilder.toString());
        }
        return null;
    }

    private void renderTree(TreeNode node, String indent, boolean isLast) {
        String name = node.getName();
        String val = (node.getValue() != null && !node.getValue().equals(name)) ? " [" + node.getValue() + "]" : "";
        System.out.println(indent + (isLast ? "└── " : "├── ") + name + val);
        List<TreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            renderTree(children.get(i), indent + (isLast ? "    " : "│   "), i == children.size() - 1);
        }
    }

    private String mapTokenToTerminal(Token token) {
        if (token.getValue().equals("#")) return "#";
        return switch (token.getType()) {
            case "关键字", "运算符", "界符" -> token.getValue();
            case "标识符", "数字" -> "i";
            default -> throw new LexException("Unrecognized token: " + token.getValue());
        };
    }
}