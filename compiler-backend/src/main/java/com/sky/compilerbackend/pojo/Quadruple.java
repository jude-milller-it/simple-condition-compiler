package com.sky.compilerbackend.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 四元式：(操作符, 操作数1, 操作数2, 结果)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quadruple {
    /** 操作符：+/>/!=/赋值/jump等 */
    private String op;
    /** 操作数1 */
    private String arg1;
    /** 操作数2 */
    private String arg2;
    /** 结果 */
    private String result;
    /** 四元式序号 */
    private int index;
    /** 所属的块的序号 */
    private int blockIndex;
}
