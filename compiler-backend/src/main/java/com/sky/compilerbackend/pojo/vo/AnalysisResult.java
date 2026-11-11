package com.sky.compilerbackend.pojo.vo;


import com.sky.compilerbackend.pojo.Symbol;
import com.sky.compilerbackend.pojo.TreeNode;
import com.sky.compilerbackend.util.SymbolTable;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
/**
 * 语法分析结果
 */
public class AnalysisResult {
    private TreeNode treeNode;          // 语法树根节点
    private List<Symbol> symbols; // 符号表
    private String log;          // 日志
    private SymbolTable symbolTable; //供semanticService使用
}
