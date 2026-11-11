package com.sky.compilerbackend.controller;

import com.sky.compilerbackend.pojo.*;
import com.sky.compilerbackend.pojo.vo.*;
import com.sky.compilerbackend.exception.LexException;
import com.sky.compilerbackend.exception.SemanticException;
import com.sky.compilerbackend.exception.SyntaxException;
import com.sky.compilerbackend.service.*;
import com.sky.compilerbackend.util.SymbolTable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 编译器核心接口控制器
 * 前端请求入口，统一处理编译流程
 */
@RestController
@RequestMapping("/compiler")
@Slf4j
public class CompilerController {

    @Resource(name = "lexerServiceImpl1")
    private LexerService lexerService;
    @Autowired
    private ParserService parserService;
    @Autowired
    private SemanticService semanticService;
    @Autowired
    private IRService irService;
    @Autowired
    private OptimizeService optimizeService;
    @Autowired
    private X86AsmGenService x86AsmGenService;

    /**
     * 全流程编译接口（词法→语法→语义→中间代码→优化→目标代码）
     * @param sourceCode 源代码字符串
     * @return 编译结果（含所有阶段数据）
     */
    @PostMapping("/full-compile")
    public Result fullCompile(@RequestBody String sourceCode) {
        log.info("开始full-compile, sourceCode:{}", sourceCode);
        try {
            // 1. 词法分析
            LexerResult lexerResult = lexerService.analyze(sourceCode);
            // 2. 语法分析（LR1）
            AnalysisResult analysisResult = parserService.analyze(lexerResult.getTokens());
            boolean syntaxIsNull = analysisResult == null;
            if (syntaxIsNull) {
                return Result.fail("语法分析失败：未识别的语法结构");
            }
            //3.语义分析
            TreeNode astRoot = analysisResult.getTreeNode();
            List<Symbol> symbols = analysisResult.getSymbols();
            SymbolTable symbolTable = analysisResult.getSymbolTable();
            SemanticResult semanticResult = semanticService.analyze(astRoot, symbols);

            //4. 中间代码生成
            List<Quadruple> quadruples1 = irService.generateQuadruples(astRoot, symbolTable);
            //5 优化
            OptimizedResult optimizedResult = optimizeService.optimize(quadruples1);
            //6 目标代码生成
            String x86AsmCode = x86AsmGenService.generateAsm(optimizedResult.getFinalResult() ,symbolTable);

            TotalProcessResult totalProcessResult = new TotalProcessResult(
                    lexerResult.getTokens(),
                    lexerResult.getActionTable(),
                    lexerResult.getGotoTable(),
                    analysisResult.getLog(),
                    symbols,
                    astRoot,
                    quadruples1,
                    optimizedResult.getBlockResult(),
                    optimizedResult.getFinalResult(),
                    x86AsmCode
            );
            return Result.success(totalProcessResult);
        } catch (LexException e) {
            return Result.fail(e.getMessage());
        } catch (SyntaxException e) {
            return Result.fail(e.getMessage());
        } catch (SemanticException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("编译异常：" + e.getMessage());
        }
    }

    /**
     * 单独词法分析接口
     */
    @PostMapping("/lex")
    public Result lexAnalyze(@RequestBody String sourceCode) {
        log.info("开始lexAnalyze, sourceCode:{}", sourceCode);
        try {
            LexerResult lexerResult = lexerService.analyze(sourceCode);
            return Result.success(lexerResult);
        } catch (LexException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 单独语法分析接口（依赖词法分析结果）
     */
    @PostMapping("/syntax")
    public Result syntaxAnalyze(@RequestBody String sourceCode) {
        log.info("开始syntaxAnalyze, sourceCode:{}", sourceCode);
        try {
            LexerResult lexerResult = lexerService.analyze(sourceCode);
            AnalysisResult analysisResult = parserService.analyze(lexerResult.getTokens());
            System.out.println("来喽！");
            System.out.println(analysisResult);
            boolean syntaxIsNull = analysisResult == null;
            if (!syntaxIsNull) {
                return Result.success("语法分析成功", analysisResult);
            }else{
                return Result.fail("语法分析失败：不符合条件语句文法规则");
            }
        } catch (LexException | SyntaxException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 单独语义分析接口（含符号表）
     */
    @PostMapping("/semantic")
    public Result semanticAnalyze(@RequestBody String sourceCode) {
        log.info("开始semanticAnalyze, sourceCode:{}", sourceCode);
        try {
            List<Token> tokens = lexerService.analyze(sourceCode).getTokens();
            AnalysisResult analysisResult = parserService.analyze(tokens);
            TreeNode root = analysisResult.getTreeNode();
            List<Symbol> symbols = analysisResult.getSymbols();
            SemanticResult semanticResult = semanticService.analyze(root, symbols);
            return Result.success(semanticResult);
        } catch (LexException | SyntaxException |SemanticException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/ir")
    public Result irAnalyze(@RequestBody String sourceCode){
        log.info("开始irAnalyze, sourceCode:{}", sourceCode);
        try {
            List<Token> tokens = lexerService.analyze(sourceCode).getTokens();
            AnalysisResult analysisResult = parserService.analyze(tokens);
            TreeNode root = analysisResult.getTreeNode();
            List<Symbol> symbols = analysisResult.getSymbols();
            SymbolTable symbolTable = analysisResult.getSymbolTable();
            SemanticResult semanticResult = semanticService.analyze(root, symbols);

            List<Quadruple> Quadruples = irService.generateQuadruples(root, symbolTable);
            return Result.success(Quadruples);
        } catch (LexException | SyntaxException |SemanticException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/optimize")
    public Result optimize(@RequestBody String sourceCode){
        log.info("开始optimize, sourceCode:{}", sourceCode);

        log.info("开始irAnalyze, sourceCode:{}", sourceCode);
        try {
            List<Token> tokens = lexerService.analyze(sourceCode).getTokens();
            AnalysisResult analysisResult = parserService.analyze(tokens);
            TreeNode root = analysisResult.getTreeNode();
            List<Symbol> symbols = analysisResult.getSymbols();
            SymbolTable symbolTable = analysisResult.getSymbolTable();
            SemanticResult semanticResult = semanticService.analyze(root, symbols);

            List<Quadruple> quadruples1 = irService.generateQuadruples(root, symbolTable);
            OptimizedResult optimizedResult = optimizeService.optimize(quadruples1);

            return Result.success(optimizedResult);
        } catch (LexException | SyntaxException |SemanticException e) {
            return Result.fail(e.getMessage());
        }

    }

    @PostMapping("/codeGen")
    public Result codeGenerator(@RequestBody String sourceCode){
        log.info("开始codeGenerator, sourceCode:{}", sourceCode);
        try {
            List<Token> tokens = lexerService.analyze(sourceCode).getTokens();
            AnalysisResult analysisResult = parserService.analyze(tokens);
            TreeNode root = analysisResult.getTreeNode();
            List<Symbol> symbols = analysisResult.getSymbols();
            SymbolTable symbolTable = analysisResult.getSymbolTable();
            SemanticResult semanticResult = semanticService.analyze(root, symbols);

            List<Quadruple> quadruples1 = irService.generateQuadruples(root, symbolTable);
            OptimizedResult optimizedResult = optimizeService.optimize(quadruples1);
            String X86AsmCode = x86AsmGenService.generateAsm(optimizedResult.getFinalResult() ,symbolTable);
            System.out.println("要来了！！！！！！11");
            System.out.println(X86AsmCode);
//            return Result.success();
            return Result.success(X86AsmCode);
        } catch (LexException | SyntaxException |SemanticException e) {
            return Result.fail(e.getMessage());
        }
    }
}