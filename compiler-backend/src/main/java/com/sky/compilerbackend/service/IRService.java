package com.sky.compilerbackend.service;

import com.sky.compilerbackend.pojo.Quadruple;
import com.sky.compilerbackend.pojo.TreeNode;
import com.sky.compilerbackend.util.SymbolTable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中间代码生成
 * 机制：深度优先搜索（DFS）+ 后序遍历。
 * AST 根节点（S'）开始，先钻到最内层节点（如变量 i），再回溯处理父节点的运算符（+、!=、:=、>），每处理一个运算符就生成一条四元式；对 if 分支则先留空跳转目标，生成完 then 块后回填
 */
@Service
public class IRService {
    private int tempIdx = 0;   // 临时变量计数器
    private int quadIdx = 1;   // 四元式序号
    private List<Quadruple> quads = new ArrayList<>();

    // 核心方法：输入AST根节点，输出四元式
    public List<Quadruple> generateQuadruples(TreeNode root, SymbolTable symTab) {
        reset();
        // 步骤1：处理S' → if A then S
        if ("S'".equals(root.getName())) {
            // 找A节点（条件）、S节点（赋值）
            TreeNode aNode = findNode(root, "A");
            TreeNode sNode = findNode(root, "S");

            // 步骤2：生成条件表达式四元式（A → B != B）
            String condTemp = genExpr(aNode, symTab);
            // 步骤3：生成假跳转四元式（先留空，后续回填）
            int jumpIdx = quadIdx;
            addQuad("jz", condTemp, null, "");

            // 步骤4：生成赋值语句四元式（S → i:=B）
            genStmt(sNode, symTab);

            // 步骤5：回填跳转目标（指向nop）
            quads.stream().filter(q -> q.getIndex() == jumpIdx)
                    .findFirst().ifPresent(q -> q.setResult(String.valueOf(quadIdx)));
        }
        // 步骤6：添加结束标记
        addQuad("nop", null, null, null);
        return quads;
    }

    // 关键步骤1：处理表达式（A/B/F，递归生成算术/关系运算四元式）
    private String genExpr(TreeNode node, SymbolTable symTab) {
        if (node == null) return null;
        switch (node.getName()) {
            // 1. 先处理B节点（算术表达式，优先级高于A）
            case "B":
                TreeNode addOp = node.getChildren().stream()
                        .filter(n -> "+".equals(n.getName())).findFirst().orElse(null);
                if (addOp != null) {
                    List<TreeNode> kids = node.getChildren().stream()
                            .filter(n -> !"+".equals(n.getName()))
                            .collect(Collectors.toList());
                    if (kids.size() < 2) return null;
                    String leftB = genExpr(kids.get(0), symTab);
                    String rightB = genExpr(kids.get(1), symTab);
                    String tempB = "t" + tempIdx++;
                    addQuad("+", leftB, rightB, tempB);
                    return tempB;
                }
                // 无+则递归处理F
                return genExpr(node.getChildren().get(0), symTab);
            // 加break（或用return终止）

            // 2. 处理A节点（条件表达式）
            case "A":
                TreeNode relOp = node.getChildren().stream()
                        .filter(n -> "!=".equals(n.getName()) || ">".equals(n.getName())).findFirst().orElse(null);
                if (relOp != null) {
                    List<TreeNode> kids = node.getChildren().stream()
                            .filter(n -> !"!=".equals(n.getName()) && !">".equals(n.getName()))
                            .collect(Collectors.toList());
                    if (kids.size() < 2) return null;
                    String leftA = genExpr(kids.get(0), symTab);
                    String rightA = genExpr(kids.get(1), symTab);
                    String tempA = "t" + tempIdx++;
                    addQuad(relOp.getName(), leftA, rightA, tempA);
                    return tempA;
                }
                // 无关系运算则递归处理B
                return genExpr(node.getChildren().get(0), symTab);
            // 加break

            // 3. 最后处理F节点（变量/常量，优先级最低）
            case "F":
                // 先判断F节点自身是否是数字
                if (node.getChildren().get(0).getValue() != null && node.getChildren().get(0).getValue().matches("^\\d+$")) {
                    return node.getChildren().get(0).getValue();
                }
                // 再找子节点里的变量/嵌套B
                TreeNode iOrConstNode = node.getChildren().stream()
                        .filter(n -> "i".equals(n.getName()) || "B".equals(n.getName()))
                        .findFirst().orElse(null);
                if (iOrConstNode == null) return null;
                if ("i".equals(iOrConstNode.getName())) {
                    return symTab.contains(iOrConstNode.getValue()) ? iOrConstNode.getValue() : null;
                } else {
                    return genExpr(iOrConstNode, symTab);
                }

            default: return null;
        }
    }

    // 关键步骤2：处理语句（S → i:=B）
    private void genStmt(TreeNode node, SymbolTable symTab) {
        if ("S".equals(node.getName())) {
            // 找:=运算符
            TreeNode assignOp = node.getChildren().stream()
                    .filter(n -> ":=".equals(n.getName())).findFirst().orElse(null);
            if (assignOp != null) {
                // 找目标变量i、表达式B
                TreeNode iNode = node.getChildren().stream()
                        .filter(n -> "i".equals(n.getName())).findFirst().orElse(null);
                TreeNode bNode = node.getChildren().stream()
                        .filter(n -> "B".equals(n.getName())).findFirst().orElse(null);
                String target = iNode.getValue();
                String exprVal = genExpr(bNode, symTab);
                addQuad(":=", exprVal, null, target);
            }
        }
    }

    // 辅助方法（极简）
    private TreeNode findNode(TreeNode node, String name) {
        if (node == null) return null;
        if (name.equals(node.getName())) return node;
        for (TreeNode kid : node.getChildren()) {
            TreeNode found = findNode(kid, name);
            if (found != null) return found;
        }
        return null;
    }
    private void addQuad(String op, String a1, String a2, String res) {
        quads.add(new Quadruple(op, a1, a2, res, quadIdx++,1));
    }
    private void reset() {
        tempIdx = 0; quadIdx = 1; quads.clear();
    }
}