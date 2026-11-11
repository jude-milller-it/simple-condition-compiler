package com.sky.compilerbackend.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sky.compilerbackend.pojo.Quadruple;
import com.sky.compilerbackend.pojo.Symbol;
import com.sky.compilerbackend.pojo.Token;
import com.sky.compilerbackend.pojo.TreeNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 统一返回结果（Jackson序列化）
 * JsonInclude：忽略null字段，减少返回数据量
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TotalProcessResult {
    /** 词法分析*/
    private List<Token> tokens;
    private Map<Integer, Map<String, String>> actionTable;
    private Map<Integer, Map<String, Integer>> gotoTable;
    /** 语法分析*/
    private String log;          // 日志
    private List<Symbol> symbols; // 符号表
    private TreeNode treeNode;          // 语法树根节点

    /** 中间代码-原始四元式 */
    private List<Quadruple> quadruples;
    /** 中间代码-优化后四元式 */
    private List<Quadruple> blockResult;
    private List<Quadruple> finalResult;
    /** 目标代码-x86汇编 */
    private String x86AsmCode;

}