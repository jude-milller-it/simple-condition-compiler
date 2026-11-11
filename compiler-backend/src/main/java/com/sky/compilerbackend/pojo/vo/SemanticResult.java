package com.sky.compilerbackend.pojo.vo;

import com.sky.compilerbackend.pojo.Symbol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 3. 语义分析结果类（SemanticResult）
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SemanticResult {
    private String pass1;      // 赋值检查结果："赋值检查通过" / 具体错误
    private String pass2;      // if检查结果："if结构检查通过" / 具体错误
    private List<Symbol> symbols; // 透传的符号表
}
