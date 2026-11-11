package com.sky.compilerbackend.pojo.vo;

import com.sky.compilerbackend.pojo.Quadruple;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptimizedResult {
    private List<Quadruple> blockResult;
    private List<Quadruple> finalResult;

}
