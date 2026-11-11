package com.sky.compilerbackend.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 符号表项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Symbol {
    /** 变量名 */
    private String name;
    /** 类型（int） */
    private String type;
    /** 虚拟内存地址 */
    private int address;
    /** 作用域（全局） */
    private String scope;
}
