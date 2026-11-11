/**
 * 用前序遍历，因为前序遍历能先访问父节点，提前判断节点类型，再遍历子节点验证规则：
 * 1.遍历到S节点（赋值语句）时，先检查其第一个子节点（:=左边）是否为i（变量）；
 * 2.遍历到S'节点时，先检查if的下一个子节点是否为A（条件表达式节点）。
 */
package com.sky.compilerbackend.service;

import com.sky.compilerbackend.pojo.vo.SemanticResult;
import com.sky.compilerbackend.pojo.Symbol;
import com.sky.compilerbackend.pojo.TreeNode;
import com.sky.compilerbackend.exception.SemanticException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AST前序遍历 + 语义检查
 * 检查规则：
 * 1. if后必须是条件表达式（A节点）
 * 2. :=左边必须是变量
 */
@Component
public class SemanticService {
    public SemanticResult analyze(TreeNode root, List<Symbol> symbols) {
        preOrderCheck(root);
        SemanticResult semanticResult = new SemanticResult();
        semanticResult.setSymbols(symbols);
        semanticResult.setPass1("if后必须是条件表达式（A节点） 检查通过");
        semanticResult.setPass2(":=左边必须是变量 检查通过");
        return semanticResult;
    }

    /**
     * 前序遍历AST并执行语义检查
     *
     * @param root AST根节点（S'）
     */
    public void preOrderCheck(TreeNode root) {
        if (root == null) {
            return;
        }

        // 1. 访问当前节点：执行语义检查规则
        checkSemanticRules(root);

        // 2. 递归遍历所有子节点（注意：你的TreeNode添加子节点是插在头部，遍历顺序需保持一致）
        for (TreeNode child : root.getChildren()) {
            preOrderCheck(child);
        }
    }

    /**
     * 核心语义检查规则实现
     *
     * @param node 当前遍历的节点
     */
    private boolean[] checkSemanticRules(TreeNode node) {
        // 规则1：检查if后必须是条件表达式（A节点）
        boolean isOK1 = false;
        System.out.println(node.getName());
        if ("S'".equals(node.getName())) {
            isOK1 = checkIfFollowedByCondition(node);
        }

        // 规则2：检查赋值语句（S节点）中 := 左边必须是变量
        boolean isOK2 = false;
        if ("S".equals(node.getName())) {
            isOK2 = checkAssignLeftIsVariable(node);
        }
        boolean[] isOK = new boolean[2];
        isOK[0] = isOK1;
        isOK[1] = isOK2;
        return isOK;
    }

    /**
     * 规则1：检查if后必须是条件表达式（A节点）
     *
     * @param s2Node if关键字节点
     */
    private boolean checkIfFollowedByCondition(TreeNode s2Node) {

        List<TreeNode> children = s2Node.getChildren();
        // 确保if后有节点，且是A节点（条件表达式）
        if (children.size() < 2) {
            throw new SemanticException("语义错误：if后缺少条件表达式");
        }
        if (!"A".equals(children.get(1).getName())) {
            throw new SemanticException(String.format(
                    "语义错误：if后必须是条件表达式（A节点），但实际是[%s]节点",
                    children.get(1)
            ));
        }
        if (children.get(1).getChildren().size() == 1) {
            throw new SemanticException(String.format(
                    "语义错误：if后必须是条件表达式（A节点），但实际是[%s,%s]节点",
                    children.get(1).getChildren().get(0).getName(),
                    children.get(1).getChildren().get(0).getValue()
            ));
        }
        return true;
    }

    /**
     * 规则2：检查赋值语句（S节点）中 := 左边必须是变量
     *
     * @param sNode 赋值语句节点（S）
     */
    private boolean checkAssignLeftIsVariable(TreeNode sNode) {
        // S节点的子节点结构：[i, :=, B]（因addChild插在头部，实际存储顺序是[B, :=, i]）
        List<TreeNode> children = sNode.getChildren();
        if (children.size() < 3) {
            throw new SemanticException("语义错误：赋值语句（S）结构不完整");
        }

        // 找到 := 节点，取其前一个节点（左边节点）
        TreeNode assignNode = null;
        int assignIndex = -1;
        for (int i = 0; i < children.size(); i++) {
            if (":=".equals(children.get(i).getName())) {
                assignIndex = i;
                break;
            }
        }
        // 检查:=左边节点（注意你的子节点存储顺序是反的，左边节点是assignIndex+1）
        if (assignIndex - 1 < 0) {
            throw new SemanticException("语义错误：:=左边缺少变量");
        }

        TreeNode leftNode = children.get(assignIndex - 1);
        // 变量节点的特征：
        if (!"i".equals(leftNode.getName()) || leftNode.getValue().matches("^\\d+$")) {
            throw new SemanticException(String.format(
                    "语义错误：:=左边必须是变量，但实际是[%s:%s]",
                    leftNode.getName(), leftNode.getValue()
            ));
        }
        return true;
    }
}
