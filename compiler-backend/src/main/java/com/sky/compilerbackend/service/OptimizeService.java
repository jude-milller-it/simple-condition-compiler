package com.sky.compilerbackend.service;

import com.sky.compilerbackend.pojo.Quadruple;
import com.sky.compilerbackend.pojo.vo.OptimizedResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Service
public class OptimizeService {
    private static final Set<String> BRANCH_OPS = new HashSet<>();
    private static final Set<String> COMPARE_OPS = new HashSet<>();
    private static final Set<String> ARITHMETIC_OPS = new HashSet<>();

    static {
        BRANCH_OPS.add("jz");
        COMPARE_OPS.add(">");
        ARITHMETIC_OPS.add("+");
    }

    // 全局常量映射（供死分支删除使用）
    private Map<String, String> globalConstMap = new HashMap<>();


    /**
     * 基础优化方法（核心逻辑）
     */
    public OptimizedResult optimize(List<Quadruple> quadruples) {
        if (quadruples == null || quadruples.isEmpty()) {
            return null;
        }
        globalConstMap.clear(); // 每次优化清空常量映射

        // 1. 划分基本块
        List<List<Quadruple>> basicBlocks = splitBasicBlocks(quadruples);
        List<Quadruple> blockResult = getBlockResult(quadruples);

        // 2. 块内优化（修复b+c复用逻辑）
        List<Quadruple> optimized = new ArrayList<>();
        for (List<Quadruple> block : basicBlocks) {
            optimized.addAll(optimizeBlock(block));
        }

        // 3. 删除死分支
        List<Quadruple> finalResult = removeDeadBranch(optimized);

        return new OptimizedResult(blockResult, finalResult);
    }

    /**
     * 分块方法（返回带块编号的结果，供前端展示）
     */
    public List<Quadruple> getBlockResult(List<Quadruple> quadruples) {
        // 1. 先拆分基本块（按jz指令为边界）
        List<List<Quadruple>> basicBlocks = splitBasicBlocks(quadruples);

        // 2. 为每个四元式设置chunkIndex（块序号），并组装最终列表
        List<Quadruple> result = new ArrayList<>();
        // 块序号从1开始（符合前端/业务的直观认知）
        int chunkIndex = 1;

        for (List<Quadruple> block : basicBlocks) {
            for (Quadruple quad : block) {
                // 为当前四元式设置所属块序号
                quad.setBlockIndex(chunkIndex);
                result.add(quad);
            }
            // 下一个块序号+1
            chunkIndex++;
        }

        return result;
    }

    /**
     * 划分基本块（按jz拆分）
     */
    private List<List<Quadruple>> splitBasicBlocks(List<Quadruple> quadruples) {
        List<List<Quadruple>> blocks = new ArrayList<>();
        List<Quadruple> currentBlock = new ArrayList<>();

        for (Quadruple quad : quadruples) {
            if (BRANCH_OPS.contains(quad.getOp())) {
                currentBlock.add(quad);
                blocks.add(new ArrayList<>(currentBlock));
                currentBlock.clear();
            } else {
                currentBlock.add(quad);
            }
        }
        if (!currentBlock.isEmpty()) {
            blocks.add(currentBlock);
        }
        return blocks;
    }

    /**
     * 块内优化（核心修复：b+c复用）
     */
    private List<Quadruple> optimizeBlock(List<Quadruple> block) {
        List<Quadruple> optimizedBlock = new ArrayList<>();
        // 表达式→临时变量映射（块内全局变量，比如b+c→t0）
        Map<String, String> exprToTemp = new HashMap<>();

        for (Quadruple quad : block) {
            String op = quad.getOp();
            String arg1 = quad.getArg1();
            String arg2 = quad.getArg2();
            String result = quad.getResult();
            int index = quad.getIndex();

            // 常量传播（把已确定的常量替换成值）
            String newArg1 = globalConstMap.getOrDefault(arg1, arg1);
            String newArg2 = globalConstMap.getOrDefault(arg2, arg2);

            // 1：比较运算常量合并（3>4→false）
            if (COMPARE_OPS.contains(op) && isNumber(newArg1) && isNumber(newArg2)) {
                String compareResult = calculateCompare(op, newArg1, newArg2);
                globalConstMap.put(result, compareResult);
                optimizedBlock.add(new Quadruple("=", compareResult, null, result, index,1));
                continue;
            }

            // 2：算术运算（b+c）复用核心逻辑
            if (ARITHMETIC_OPS.contains(op) && newArg1 != null && newArg2 != null) {
                // 生成表达式唯一标识：+_b_c
                String exprKey = op + "_" + newArg1 + "_" + newArg2;
                // 如果该表达式已计算过，直接复用临时变量
                if (exprToTemp.containsKey(exprKey)) {
                    String reusedTemp = exprToTemp.get(exprKey);
                    globalConstMap.put(result, reusedTemp);
                    // 生成赋值四元式（t2 = t0），不再重复计算b+c
                    optimizedBlock.add(new Quadruple("=", reusedTemp, null, result, index,1));
                    continue;
                }
                // 首次计算，记录表达式→临时变量的映射
                exprToTemp.put(exprKey, result);
            }

            // 保留优化后的四元式
            Quadruple newQuad = new Quadruple(op, newArg1, newArg2, result, index,1);
            optimizedBlock.add(newQuad);
        }
        return optimizedBlock;
    }

    /**
     * 删除死分支（jz false）
     */
    private List<Quadruple> removeDeadBranch(List<Quadruple> quadruples) {
        List<Quadruple> result = new ArrayList<>();
        boolean skipCode = false;

        for (Quadruple quad : quadruples) {
            String op = quad.getOp();
            String arg1 = quad.getArg1();

            if ("jz".equals(op)) {
                String realArg1 = globalConstMap.getOrDefault(arg1, arg1);
                if ("false".equals(realArg1)) {
                    skipCode = true;
                    continue;
                }
            }

            if ("nop".equals(op)) {
                skipCode = false;
            }

            if (!skipCode) {
                result.add(quad);
            }
        }
        return result;
    }

    // 计算比较结果
    private String calculateCompare(String op, String val1Str, String val2Str) {
        int val1 = Integer.parseInt(val1Str);
        int val2 = Integer.parseInt(val2Str);
        return ">".equals(op) ? String.valueOf(val1 > val2) : "";
    }

    // 判断是否为数字
    private boolean isNumber(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}