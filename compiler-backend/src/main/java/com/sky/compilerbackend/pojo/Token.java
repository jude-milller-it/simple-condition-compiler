package com.sky.compilerbackend.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 词法分析二元式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    /** 单词类型：关键字/标识符/运算符/界符/数字 */
    private String type;
    /** 单词值 */
    private String value;
    /** 行号（用于错误定位） */
    private int line;
}
