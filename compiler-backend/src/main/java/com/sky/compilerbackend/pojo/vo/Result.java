package com.sky.compilerbackend.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 统一返回结果（Jackson序列化）
 * JsonInclude：忽略null字段，减少返回数据量
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result {
    /** 操作是否成功 */
    private boolean success;
    /** 中文错误提示 */
    private String message;
    /** 词法分析-二元式序列 */
    private Object data;


    public static Result success() {
        return new Result(true, "success", null);
    }


    public static Result success(Object data) {
        return new Result(true, "success", data);
    }
    public static Result success(String message, Object data) {
        return new Result(true, message, data);
    }


    public static Result fail(String message) {
        return new Result(false, message, null);
    }
}