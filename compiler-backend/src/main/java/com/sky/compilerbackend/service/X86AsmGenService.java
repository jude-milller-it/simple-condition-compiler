package com.sky.compilerbackend.service;

import com.sky.compilerbackend.pojo.Quadruple;
import com.sky.compilerbackend.pojo.Symbol;
import com.sky.compilerbackend.util.SymbolTable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 64位x86汇编生成服务
 * 修复：通过 StringBuilder 拼接彻底解决 String.format 导致的 %r 转义异常
 */
@Component
public class X86AsmGenService {

    // 64位全局数据基址寄存器（rbx）
    private static final String GLOBAL_DATA_BASE_REG = "%rbx";
    // int占4字节
    private static final int INT_SIZE = 4;

    /**
     * 核心：生成64位x86汇编代码
     */
    public String generateAsm(List<Quadruple> quadruples, SymbolTable symbolTable) {
        StringBuilder asm = new StringBuilder();

        // 1. 预处理：收集所有变量到符号表
        preprocessVars(quadruples, symbolTable);

        // 2. 数据段生成
        asm.append("# 64位x86汇编（全局int变量）\n\n");
        asm.append(".section .data\nglobal_data:\n");

        // 遍历符号表标注偏移
        for (Symbol symbol : symbolTable.getAllSymbols()) {
            asm.append("# ").append(symbol.getName())
                    .append(": 偏移").append(symbol.getAddress())
                    .append("(").append(GLOBAL_DATA_BASE_REG).append(")\n");
        }

        // 计算需要的空间大小
        int totalSize = symbolTable.getAllSymbols().stream()
                .mapToInt(Symbol::getAddress).max().orElse(0) + INT_SIZE;
        asm.append("    .space ").append(totalSize).append("\n\n");

        // 3. 代码段入口
        asm.append(".section .text\n.global main\nmain:\n");
        asm.append("    movq $global_data, ").append(GLOBAL_DATA_BASE_REG).append("\n");

        // 4. 遍历四元式生成指令
        for (Quadruple q : quadruples) {
            String op = q.getOp();
            int idx = q.getIndex();

            // 注释部分
            asm.append("\n    # 四元式[").append(idx).append("]: ")
                    .append(op).append(", ").append(q.getArg1()).append(", ")
                    .append(q.getArg2()).append(", ").append(q.getResult()).append("\n");

            switch (op) {
                case ">":    generateGreaterOp(asm, q, symbolTable); break;
                case "!=":   generateNotEqualOp(asm, q, symbolTable); break;
                case "+":    generateAddOp(asm, q, symbolTable); break;
                case "赋值":
                case ":=":
                case "=":    generateAssignOp(asm, q, symbolTable); break;
                case "jz":   generateJumpZeroOp(asm, q, symbolTable); break;
                case "jnz":  generateJumpNotZeroOp(asm, q, symbolTable); break;
                case "nop":  asm.append("    .L").append(idx).append(": nop\n"); break;
                default:     asm.append("    # 暂不支持操作符: ").append(op).append("\n");
            }
        }

        // 5. 退出程序
        asm.append("\n    # 程序退出\n");
        asm.append("    movq $60, %rax\n");
        asm.append("    xorq %rdi, %rdi\n");
        asm.append("    syscall\n");

        return asm.toString();
    }

    private void preprocessVars(List<Quadruple> quadruples, SymbolTable st) {
        for (Quadruple q : quadruples) {
            String[] vars = {q.getArg1(), q.getArg2(), q.getResult()};
            for (String var : vars) {
                if (var != null && !var.isEmpty() && !isNumber(var) && !var.equalsIgnoreCase("null")) {
                    if (!st.contains(var)) {
                        st.addSymbol(var, "int", "global");
                    }
                }
            }
        }
    }

    // ------------------------ 指令实现（全部改用拼接，防止%转义错误） ------------------------

    private void generateGreaterOp(StringBuilder asm, Quadruple q, SymbolTable st) {
        loadVarToReg(asm, q.getArg1(), "%rax", st);
        if (isNumber(q.getArg2())) {
            asm.append("    cmpq $").append(q.getArg2()).append(", %rax\n");
        } else {
            loadVarToReg(asm, q.getArg2(), "%rcx", st);
            asm.append("    cmpq %rcx, %rax\n");
        }
        asm.append("    setg %al\n");
        asm.append("    movzbl %al, %rax\n");
        storeRegToVar(asm, "%rax", q.getResult(), st);
    }

    private void generateNotEqualOp(StringBuilder asm, Quadruple q, SymbolTable st) {
        loadVarToReg(asm, q.getArg1(), "%rax", st);
        if (isNumber(q.getArg2())) {
            asm.append("    cmpq $").append(q.getArg2()).append(", %rax\n");
        } else {
            loadVarToReg(asm, q.getArg2(), "%rcx", st);
            asm.append("    cmpq %rcx, %rax\n");
        }
        asm.append("    setne %al\n");
        asm.append("    movzbl %al, %rax\n");
        storeRegToVar(asm, "%rax", q.getResult(), st);
    }

    private void generateAddOp(StringBuilder asm, Quadruple q, SymbolTable st) {
        loadVarToReg(asm, q.getArg1(), "%rax", st);
        if (isNumber(q.getArg2())) {
            asm.append("    addq $").append(q.getArg2()).append(", %rax\n");
        } else {
            loadVarToReg(asm, q.getArg2(), "%rcx", st);
            asm.append("    addq %rcx, %rax\n");
        }
        storeRegToVar(asm, "%rax", q.getResult(), st);
    }

    private void generateAssignOp(StringBuilder asm, Quadruple q, SymbolTable st) {
        loadVarToReg(asm, q.getArg1(), "%rax", st);
        storeRegToVar(asm, "%rax", q.getResult(), st);
    }

    private void generateJumpZeroOp(StringBuilder asm, Quadruple q, SymbolTable st) {
        loadVarToReg(asm, q.getArg1(), "%rax", st);
        asm.append("    testq %rax, %rax\n");
        asm.append("    jz .L").append(q.getResult()).append("\n");
    }

    private void generateJumpNotZeroOp(StringBuilder asm, Quadruple q, SymbolTable st) {
        loadVarToReg(asm, q.getArg1(), "%rax", st);
        asm.append("    testq %rax, %rax\n");
        asm.append("    jnz .L").append(q.getResult()).append("\n");
    }

    // ------------------------ 极简辅助方法 ------------------------

    private void loadVarToReg(StringBuilder asm, String var, String reg, SymbolTable st) {
        if (isNumber(var)) {
            asm.append("    movq $").append(var).append(", ").append(reg).append("\n");
        } else {
            Symbol sym = st.getAllSymbols().stream()
                    .filter(s -> s.getName().equals(var)).findFirst()
                    .orElseThrow(() -> new RuntimeException("变量未定义：" + var));
            asm.append("    movq ").append(sym.getAddress())
                    .append("(").append(GLOBAL_DATA_BASE_REG).append("), ")
                    .append(reg).append("\n");
        }
    }

    private void storeRegToVar(StringBuilder asm, String reg, String var, SymbolTable st) {
        Symbol sym = st.getAllSymbols().stream()
                .filter(s -> s.getName().equals(var)).findFirst()
                .orElseThrow(() -> new RuntimeException("变量未定义：" + var));
        asm.append("    movq ").append(reg).append(", ")
                .append(sym.getAddress()).append("(").append(GLOBAL_DATA_BASE_REG).append(")\n");
    }

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