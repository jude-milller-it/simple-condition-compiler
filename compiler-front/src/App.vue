<template>
  <div class="compiler-app">
    <el-header class="header">
      <h1>条件语句编译器（支持 >、!=、+）</h1>
      <br />
      <h4>by jude-milller-it</h4>
    </el-header>

    <el-container class="main-container">
      <el-aside width="35%" class="editor-aside">

          
        <el-card class="box-card editor-card">

          <template #header>
            <div class="card-header">
              <span>源代码编辑</span>
              <el-button type="danger" size="small" @click="clearCode" plain
                >清空代码</el-button
              >
            </div>
          </template>

          <el-button-group class="btn-group">
            <el-button type="success" @click="handleLex" :loading="loading"
              >词法分析</el-button
            >
            <el-button type="warning" @click="handleSyntax" :loading="loading"
              >语法分析</el-button
            >
            <el-button type="info" @click="handleSemantic">语义分析</el-button>
            <el-button type="primary" @click="handIR" :loading="loading"
              >中间代码生成</el-button
            >
            <el-button type="success" @click="handleOptimize" :loading="loading"
              >中间代码优化</el-button
            >
            <el-button type="warning" @click="handleCodeGen" :loading="loading"
              >目标代码生成</el-button
            >
            <el-button
              type="danger"
              @click="handleFullCompile"
              :loading="loading"
              >全流程编译</el-button
            >
          </el-button-group>

          <textarea
            v-model="sourceCode"
            class="code-editor"
            placeholder="请输入待编译的代码，示例：
if priceC!=(priceA+priceB)
then priceC:=(priceA + 1)"
          ></textarea>

          <el-alert
            v-if="errorMsg"
            title="编译异常"
            type="error"
            :description="errorMsg"
            show-icon
            style="margin-top: 15px"
          />
        </el-card>
        <br>

                <!-- 新增：文法规则展示区域 -->
        <div class="grammar-rule-container">
          <div class="grammar-title">文法规则</div>
          <div class="grammar-content">
            <div class="grammar-item">M → S'</div>
            <div class="grammar-item">S' → if A then S</div>
            <div class="grammar-item">S → i := B</div>
            <div class="grammar-item">A → B != B | B > B | B</div>
            <div class="grammar-item">B → B + F | F</div>
            <div class="grammar-item">F → i | ( B )</div>
            <div class="grammar-note">
              <span class="note-label">终结符：</span>M, S’， S， A， B， F
            </div>
            <div class="grammar-note">
              <span class="note-label">非终结符：</span>if ，then , :=, != , +, > , ( , )
            </div>
          </div>
        </div>


      </el-aside>

      <el-aside width="65%" class="result-aside">
        <el-card v-if="tokens.length" class="result-card">
          <template #header>1.1 词法分析 - 二元式序列</template>
          <el-table :data="tokens" border stripe size="small" max-height="300">
            <el-table-column prop="type" label="单词类型" align="center" />
            <el-table-column prop="value" label="单词值" align="center" />
            <el-table-column prop="line" label="行号" align="center" />
          </el-table>
        </el-card>

        <el-card
          v-if="
            Object.keys(actionTable).length || Object.keys(gotoTable).length
          "
          class="result-card"
        >
          <template #header>词法分析 - LR分析表（Action + Goto）</template>

          <!-- Action表（行：状态，列：符号） -->
          <div class="table-group">
            <div class="table-title">1.2 Action 表</div>
            <el-table
              :data="actionTableData"
              border
              stripe
              size="small"
              max-height="300"
            >
              <!-- 第一列：状态 -->
              <el-table-column
                prop="state"
                label="状态"
                width="80"
                align="center"
              />
              <!-- 动态生成符号列 -->
              <el-table-column
                v-for="symbol in actionSymbols"
                :key="`action-${symbol}`"
                :label="symbol"
                align="center"
              >
                <template #default="{ row }">
                  {{ row.actions?.[symbol] || "-" }}
                  <!-- 增加可选链，防止空值 -->
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- Goto表（行：状态，列：符号） -->
          <div class="table-group" style="margin-top: 20px">
            <div class="table-title">1.3 Goto 表</div>
            <el-table
              :data="gotoTableData"
              border
              stripe
              size="small"
              max-height="300"
            >
              <!-- 第一列：状态 -->
              <el-table-column
                prop="state"
                label="状态"
                width="80"
                align="center"
              />
              <!-- 动态生成符号列 -->
              <el-table-column
                v-for="symbol in gotoSymbols"
                :key="`goto-${symbol}`"
                :label="symbol"
                align="center"
              >
                <template #default="{ row }">
                  {{ row.gotos?.[symbol] || "-" }}
                  <!-- 增加可选链，防止空值 -->
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>

        <!-- 以下是语法分析 -->
        <div v-if="syntaxLog" class="log-section">
          <h4 class="section-title">2.1 语法分析日志</h4>
          <pre class="log-pre">{{ syntaxLog }}</pre>
        </div>

        <!-- 核心：递归模板 - 实现左右子树横向布局 -->
        <el-card v-if="syntaxTree" class="result-card">
          <template #header>2.2 可视化语法树 (AST)</template>
          <div class="ast-tree-container">
            <div class="ast-tree">
              <ASTItem :node="syntaxTree" :depth="0" />
            </div>
          </div>
        </el-card>

        <!-- 以下是语义分析 -->
        <el-card v-if="symbolTable.length" class="result-card">
          <template #header>3. 语法分析 - 符号表</template>
          <el-table :data="symbolTable" border stripe size="small">
            <el-table-column prop="name" label="变量名" align="center" />
            <el-table-column prop="type" label="类型" align="center" />
            <el-table-column prop="address" label="虚拟地址" align="center" />
            <el-table-column prop="scope" label="作用域" align="center" />
          </el-table>
        </el-card>

        <!-- 以下是中间代码（四元式）生成 -->
        <el-card v-if="quadTable.length" class="result-card">
          <template #header>4. 中间代码生成 - 四元式表</template>
          <el-table :data="quadTable" border stripe size="small">
            <el-table-column prop="index" label="索引" align="center" />
            <el-table-column prop="op" label="操作符" align="center" />
            <el-table-column prop="arg1" label="操作数1" align="center" />
            <el-table-column prop="arg2" label="操作数2" align="center" />
            <el-table-column prop="result" label="结果" align="center" />
          </el-table>
        </el-card>

        <!-- 以下是中间代码优化 -->
        <el-card v-if="blockTable.length" class="result-card">
          <template #header>5.1 中间代码优化 - 四元式表分块</template>
          <el-table
            :data="blockTable"
            border
            size="small"
            :row-class-name="getBlockRowClass"
          >
            <el-table-column prop="index" label="索引" align="center" />
            <el-table-column prop="op" label="操作符" align="center" />
            <el-table-column prop="arg1" label="操作数1" align="center" />
            <el-table-column prop="arg2" label="操作数2" align="center" />
            <el-table-column prop="result" label="结果" align="center" />
          </el-table>
        </el-card>
        <el-card v-if="optimizedTable.length" class="result-card">
          <template #header>5.2 中间代码优化 - 四元式表块内优化</template>
          <el-table :data="optimizedTable" border stripe size="small">
            <el-table-column prop="index" label="索引" align="center" />
            <el-table-column prop="op" label="操作符" align="center" />
            <el-table-column prop="arg1" label="操作数1" align="center" />
            <el-table-column prop="arg2" label="操作数2" align="center" />
            <el-table-column prop="result" label="结果" align="center" />
          </el-table>
        </el-card>

        <!-- 以下是目标代码生成 -->
        <!-- 新增：目标代码（汇编）高亮展示 -->
        <el-card v-if="targetCode" class="result-card">
          <template #header>6. 目标代码生成 - 64位x86汇编代码</template>
          <!-- 汇编代码高亮容器 -->
          <div class="asm-code-container">
            <pre class="asm-code-pre" v-html="highlightedAsmCode"></pre>
          </div>
          <!-- 复制按钮 -->
          <el-button
            type="text"
            icon="el-icon-copy-document"
            @click="copyTargetCode"
            style="margin-top: 10px; padding: 0"
          >
            复制汇编代码
          </el-button>
        </el-card>
      </el-aside>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, defineComponent, getCurrentInstance } from "vue";
import { ElMessage } from "element-plus";
import {
  fullCompile,
  lexAnalyze,
  syntaxAnalyze,
  semanticAnalyze,
  irAnalyze,
  optimizeAnalyze,
  codeGenAnalyze,
} from "./api/compilerApi";

import ASTItem from "./components/ASTItem.vue";

// 响应式数据（统一类型，避免切换）
const sourceCode = ref("if priceC!=(priceA+priceB)\nthen priceC:=(priceA + 1)");
const errorMsg = ref("");
const tokens = ref([]);
const actionTable = ref({}); // 固定为对象
const gotoTable = ref({}); // 固定为对象

const symbolTable = ref([]); // 固定为数组

const syntaxLog = ref("");
const syntaxTree = ref(null);

const targetCode = ref(""); // 原始汇编字符串

// 响应式变量：存储Action/Goto表的符号列
const actionSymbols = ref([]);
const gotoSymbols = ref([]);

const quadTable = ref([]); // 固定为数组
const blockTable = ref([]);
const optimizedTable = ref([]);

// 加载状态变量（全局）
const loading = ref(false); // 标记是否正在请求后端

// 格式化树结构
const formatTreeData = (node) => {
  if (!node) return null;
  return {
    name: node.name || "Node",
    value: node.value || "",
    children: Array.isArray(node.children)
      ? node.children.map(formatTreeData)
      : [],
  };
};

// 重置界面（关键：保持数据类型一致）
const resetResult = () => {
  errorMsg.value = "";
  tokens.value = [];
  actionTable.value = {}; // 重置为对象，而非数组
  gotoTable.value = {}; // 重置为对象，而非数组

  symbolTable.value = [];
  quadTable.value = [];
  blockTable.value = [];
  optimizedTable.value = [];
  syntaxLog.value = "";
  syntaxTree.value = null;
  actionSymbols.value = []; // 清空符号列
  gotoSymbols.value = []; // 清空符号列
  targetCode.value = ""; // 清空符号列
};

const clearCode = () => {
  sourceCode.value = "";
  resetResult();
};

// 词法分析处理
const handleLex = async () => {
  resetResult();
  loading.value = true; // 开启加载
  try {
    const res = await lexAnalyze(sourceCode.value);
    if (res.success) {
      tokens.value = res.data.tokens || [];
      actionTable.value = res.data.actionTable || {}; // 兜底为对象
      gotoTable.value = res.data.gotoTable || {}; // 兜底为对象
      ElMessage.success("词法分析完成");
    } else {
      errorMsg.value = res.message;
    }
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false; // 无论成功/失败，最终关闭加载
  }
};

// 处理Action表：行=状态，列=符号，值=动作（增加空值保护）
const actionTableData = computed(() => {
  if (typeof actionTable.value !== "object" || actionTable.value === null) {
    return [];
  }
  const data = [];
  const symbols = new Set();

  // 1. 收集所有符号（列）
  Object.entries(actionTable.value).forEach(([state, symbolMap]) => {
    if (symbolMap && typeof symbolMap === "object") {
      // 空值保护
      Object.keys(symbolMap).forEach((symbol) => symbols.add(symbol));
    }
  });
  actionSymbols.value = Array.from(symbols).sort();

  // 2. 构建表格数据
  Object.entries(actionTable.value).forEach(([state, symbolMap]) => {
    data.push({
      state,
      actions: symbolMap || {}, // 空值保护
    });
  });
  return data.sort((a, b) => Number(a.state) - Number(b.state));
});

// 处理Goto表：行=状态，列=符号，值=下一状态（增加空值保护）
const gotoTableData = computed(() => {
  if (typeof gotoTable.value !== "object" || gotoTable.value === null) {
    return [];
  }
  const data = [];
  const symbols = new Set();

  // 1. 收集所有符号（列）
  Object.entries(gotoTable.value).forEach(([state, symbolMap]) => {
    if (symbolMap && typeof symbolMap === "object") {
      // 空值保护
      Object.keys(symbolMap).forEach((symbol) => symbols.add(symbol));
    }
  });
  gotoSymbols.value = Array.from(symbols).sort();

  // 2. 构建表格数据
  Object.entries(gotoTable.value).forEach(([state, symbolMap]) => {
    data.push({
      state,
      gotos: symbolMap || {}, // 空值保护
    });
  });
  return data.sort((a, b) => Number(a.state) - Number(b.state));
});

// 语法分析处理 (包含符号表和树)
const handleSyntax = async () => {
  resetResult();
  loading.value = true; // 开启加载
  try {
    const res = await syntaxAnalyze(sourceCode.value);
    const resultData = res.data || {}; // 空值保护

    if (res.success) {
      syntaxLog.value = resultData.log || "";
      syntaxTree.value = formatTreeData(resultData.treeNode);
      symbolTable.value = resultData.symbols || [];
      ElMessage.success("语法分析完成");
    } else {
      errorMsg.value = res.message;
      if (resultData) syntaxLog.value = resultData.log || "";
    }
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};

// // 新增：获取全局 app 实例，注册 ASTItem 为全局组件
// const { appContext } = getCurrentInstance();

// 注册为全局组件
// appContext.app.component("ASTItem", ASTItem);

//语义分析
const handleSemantic = async () => {
  resetResult();
  loading.value = true; // 开启加载
  try {
    const res = await semanticAnalyze(sourceCode.value);
    const resultData = res.data || {}; // 空值保护

    if (res.success) {
      symbolTable.value = resultData.symbols || [];
      ElMessage.success("if结构检查通过");
      ElMessage.success("赋值检查通过");
    } else {
      errorMsg.value = res.message;
    }
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};

//中间代码生成
const handIR = async () => {
  resetResult();
  loading.value = true; // 开启加载
  try {
    const res = await irAnalyze(sourceCode.value);
    const resultData = res.data || {}; // 空值保护

    if (res.success) {
      quadTable.value = resultData || [];
      ElMessage.success("中间代码生成完成");
    } else {
      errorMsg.value = res.message;
    }
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};

const handleOptimize = async () => {
  resetResult();
  loading.value = true; // 开启加载
  try {
    const res = await optimizeAnalyze(sourceCode.value);
    const resultData = res.data || {}; // 空值保护

    if (res.success) {
      blockTable.value = resultData.blockResult || [];
      optimizedTable.value = resultData.finalResult || [];
      ElMessage.success("中间代码优化完成");
    } else {
      errorMsg.value = res.message;
    }
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};
/* 四元式表分块 */
// 1. 定义块颜色映射（柔和色系，支持多块循环）
const blockColors = [
  "block-color-1",
  "block-color-2",
  "block-color-3",
  "block-color-4",
];

// 2. 行样式函数：根据blockIndex返回对应颜色类名
const getBlockRowClass = ({ row }) => {
  console.log("row.index:", row.index);
  console.log("(row.blockIndex) - 1: ", row.blockIndex - 1);

  // 取blockIndex，避免0的情况，从1开始计算
  const index = (row.blockIndex || 1) - 1;

  console.log(
    "blockColors[index % blockColors.length]: ",
    blockColors[index % blockColors.length]
  );

  // 循环取颜色（支持超过4个块的情况）
  return blockColors[index % blockColors.length];
};

// 新增：目标代码相关响应式数据
// 计算属性：高亮后的汇编代码（HTML格式）
const highlightedAsmCode = computed(() => {
  if (!targetCode.value) return "";

  let code = targetCode.value.replace(/\n/g, "<br>").replace(/\s/g, "&nbsp;");

  code = code
    // 1. 注释
    .replace(/(#.*?$)/gms, '<span class="asm-comment">$1</span>')
    // 2. 标签
    .replace(/([a-zA-Z0-9_]+\:)/g, '<span class="asm-label">$1</span>')
    // 3. 段声明/伪指令
    .replace(
      /(\.section|\.global|\.data|\.text|\.space|\.globl)/g,
      '<span class="asm-directive">$1</span>'
    )
    // 4. 内存寻址（包含数字）
    .replace(/(\d+&nbsp;\(%[a-z0-9]+\))/g, '<span class="asm-memory">$1</span>')
    // 5. 数字常量（含$前缀）
    .replace(/(\$?\d+)/g, '<span class="asm-constant">$1</span>')
    // 6. 寄存器
    .replace(/(%[a-z0-9]+)/g, '<span class="asm-register">$1</span>')
    // 7. 指令（含后缀）
    .replace(
      /(mov[qlb]?|add[qlb]?|sub[qlb]?|mul[qlb]?|div[qlb]?|cmp[qlb]?|setg|setl|movzbl|test[qlb]?|jz|jnz|jmp|push[qlb]?|pop[qlb]?|call|ret|syscall|xor[qlb]?|and[qlb]?|or[qlb]?)/g,
      '<span class="asm-instruction">$1</span>'
    );

  return code;
});

// 新增：目标代码生成方法
const handleCodeGen = async () => {
  resetResult();
  loading.value = true;
  try {
    const res = await codeGenAnalyze(sourceCode.value);
    const resultData = res.data || {};
    if (res.success) {
      targetCode.value = resultData || ""; // 后端返回的汇编字符串
      ElMessage.success("目标代码生成完成");
    } else {
      errorMsg.value = res.message;
    }
  } catch (err) {
    errorMsg.value = err.message;
  } finally {
    loading.value = false;
  }
};

// 新增：复制汇编代码方法
const copyTargetCode = () => {
  if (!targetCode.value) {
    ElMessage.warning("暂无汇编代码可复制");
    return;
  }
  // 复制到剪贴板
  navigator.clipboard
    .writeText(targetCode.value)
    .then(() => {
      ElMessage.success("汇编代码已复制到剪贴板");
    })
    .catch(() => {
      ElMessage.error("复制失败，请手动复制");
    });
};

const handleFullCompile = async () => {
  resetResult();
  loading.value = true; // 开启加载
  try {
    const res = await fullCompile(sourceCode.value);
    const resultData = res.data || {}; // 空值保护
    if (res.success) {
      tokens.value = resultData.tokens || [];
      //1 词法分析
      actionTable.value = resultData.actionTable || {}; // 兜底为对象
      gotoTable.value = resultData.gotoTable || {}; // 兜底为对象
      ElMessage.success("1.词法分析完成");

      //2.语法分析
      syntaxLog.value = resultData.log || "";
      syntaxTree.value = formatTreeData(resultData.treeNode);
      ElMessage.success("2.语法分析完成");

      //3.语义分析
      symbolTable.value = resultData.symbols || [];
      ElMessage.success("3.1 语义分析：if结构检查通过");
      ElMessage.success("3.2 语义分析：赋值检查通过");

      //4.中间代码生成
      quadTable.value = resultData.quadruples || [];
      ElMessage.success("4.中间代码生成完成");

      //5.代码优化
      blockTable.value = resultData.blockResult || [];
      optimizedTable.value = resultData.finalResult || [];
      ElMessage.success("5.中间代码优化完成");

      //6.目标代码生成
      targetCode.value = resultData.x86AsmCode || ""; // 后端返回的汇编字符串
      ElMessage.success("6. 目标代码生成完成");
    } else {
      errorMsg.value = res.message;
    }
  } catch (err) {
    errorMsg.value = err.message;
    if (resultData) syntaxLog.value = resultData.log || "";
  } finally {
    loading.value = false; // 无论成功/失败，最终关闭加载
  }
};
</script>

<style scoped>
/* 文法规则说明部分 */
/* 文法规则容器样式 */
.grammar-rule-container {
  margin-bottom: 15px;
  padding: 12px 15px;
  background-color: #ffffff;
  border-radius: 6px;
  /* border-left: 4px solid #409eff; */
}

/* 文法标题样式 */
.grammar-title {
  font-size: 16px;
  font-weight: 400;
  color: #303133;
  margin-bottom: 8px;
}

/* 文法规则条目样式 */
.grammar-content {
  font-family: "Consolas", "Liberation Serif", monospace;
  font-size: 14px;
  color: #4e5969;
  line-height: 1.8;
}

.grammar-item {
  padding: 2px 0;
}

/* 备注样式 */
.grammar-note {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed #e6e6e6;
}

.note-label {
  font-weight: 600;
  color: #409eff;
}


/* 四元式表分块 */
/* 块颜色样式：必须用 ::v-deep 穿透 scoped */
:deep(.block-color-1) {
  background-color: #f9f8dc !important; /* 淡蓝 */
}
:deep(.block-color-2) {
  background-color: #d8f5e0 !important; /* 更淡蓝 */
}
:deep(.block-color-3) {
  background-color: #f5e9f7 !important; /* 淡粉 */
}
:deep(.block-color-4) {
  background-color: #ddf7db !important; /* 浅灰 */
}

/**.ast-tree */

/* 横向语法树核心样式 - 适配左/右子树 */
.ast-tree-container {
  background: #f8f9fa;
  padding: 40px;
  border-radius: 4px;
  overflow: auto; /* 确保双向滚动 */
  display: block; /* 确保它是一个块级容器 */
}

.ast-tree {
  display: inline-block; /* 关键：让树根据内容自适应宽度，而不是根据父容器 */
  min-width: 100%; /* 初始至少占满宽度 */
  text-align: center;
}

/* 单个节点容器 */
.tree-item {
  position: relative;
  margin: 0 auto;
  --depth: 0; /* 层级变量 */
}

/* 节点盒子（居中） */
.node-box {
  display: flex;
  justify-content: center;
  margin: 10px 0;
}

/* 节点内容 */
.node-content {
  display: inline-flex;
  align-items: center;
  padding: 8px 16px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  z-index: 2;
  position: relative;
}
.non-terminal-node {
  border: 2px solid #409eff;
}
.terminal-node {
  border: 2px solid #67c23a;
}
.node-name {
  font-weight: 600;
  font-size: 14px;
}
.non-terminal-node .node-name {
  color: #409eff;
}
.terminal-node .node-name {
  color: #67c23a;
}
.node-value {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
  font-style: italic;
}

/* 子节点容器 */
.children-wrapper {
  position: relative;
  z-index: 1;
}

/* 二元运算符 - 横向左右子树 */
.binary-children {
  display: flex;
  justify-content: space-around;
  align-items: flex-start;
  width: calc(200px * var(--depth) + 300px);
  margin: 0 auto;
}
.child-branch {
  position: relative;
  flex: 1;
}
/* 左右分支连线 */
.left-branch::before,
.right-branch::before {
  content: "";
  position: absolute;
  top: -10px;
  width: 50%;
  height: 1px;
  background: #dcdfe6;
}
.left-branch::before {
  right: 0;
}
.right-branch::before {
  left: 0;
}
.left-branch::after,
.right-branch::after {
  content: "";
  position: absolute;
  top: -10px;
  width: 1px;
  height: 10px;
  background: #dcdfe6;
}
.left-branch::after {
  right: 0;
}
.right-branch::after {
  left: 0;
}

/* 普通节点 - 纵向子节点 */
.normal-children {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-left: calc(var(--depth) * 24px);
}
/* 纵向分支连线 */
.normal-children .tree-item::before {
  content: "";
  position: absolute;
  left: -12px;
  top: 20px;
  width: 12px;
  height: 1px;
  background: #dcdfe6;
}
.normal-children .tree-item::after {
  content: "";
  position: absolute;
  left: -12px;
  top: 0;
  bottom: 0;
  width: 1px;
  background: #dcdfe6;
}

/* action goto表格 */
.table-group {
  background: #fff;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
.table-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #1989fa;
  border-bottom: 1px solid #f0f2f5;
  padding-bottom: 8px;
}

.result-card {
  margin-bottom: 15px;
}

.table-row {
  margin-top: 10px;
}

/* 适配小屏幕，避免表格挤压 */
@media (max-width: 768px) {
  .el-col {
    width: 100% !important;
    margin-bottom: 15px;
  }
}
.compiler-app {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.header {
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
.main-container {
  flex: 1;
  overflow: hidden;
  background: #f0f2f5;
}
.editor-aside,
.result-aside {
  padding: 15px;
  overflow-y: auto;
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.btn-group {
  margin-bottom: 15px;
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 2px; /* 按钮间统一间距，无需手动清边距 */
}
/* 第一行：前3个按钮，均分宽度 */
.btn-group .el-button:nth-child(-n + 3) {
  width: calc((100% - 2 * 8px) / 3);
  box-sizing: border-box; /* 避免边框/内边距撑宽 */
}
/* 第二行：第4-6个按钮，均分宽度 */
.btn-group .el-button:nth-child(n + 4):nth-child(-n + 6) {
  width: calc((100% - 2 * 8px) / 3);
  box-sizing: border-box;
}
/* 第三行：第7个按钮（全流程编译），单独占满整行 */
.btn-group .el-button:nth-child(7) {
  width: 100%;
  box-sizing: border-box;
  margin-top: 4px; /* 可选：和上一行拉开一点距离 */
}

.code-editor {
  width: 100%;
  height: 250px;
  background: #282c34;
  color: #abb2bf;
  padding: 12px;
  border-radius: 4px;
  font-family: "Fira Code", "Consolas", monospace;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  border: none;
  outline: none;
}

.log-section {
  margin-top: 20px;
}
.section-title {
  margin-bottom: 8px;
  color: #606266;
  font-size: 14px;
  border-left: 4px solid #409eff;
  padding-left: 8px;
}
.log-pre {
  background: #ffffff;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  color: #303133;
  border: 1px solid #dcdfe6;
  white-space: pre;
  overflow-x: auto;
  font-family: "Consolas", monospace;
}

.tree-wrapper {
  background: white;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
.node-name {
  font-weight: bold;
}
.non-terminal {
  color: #409eff;
}
.terminal {
  color: #67c23a;
}
.node-value {
  color: #909399;
  font-style: italic;
  font-size: 12px;
}

.placeholder-card {
  opacity: 0.8;
}

/** 以下是目标代码代码块 */
.asm-code-container {
  background: #282c34;
  border-radius: 4px;
  padding: 16px;
  margin-top: 10px;
}
.asm-code-pre {
  margin: 0;
  color: #abb2bf;
  font-family: "Fira Code", "Consolas", monospace;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre;
  overflow-x: auto;
}
/* 注释高亮 */
:deep(.asm-comment) {
  color: #6a9955;
}
/* 段声明/伪指令高亮 */
:deep(.asm-directive) {
  color: #c586c0;
}
/* 寄存器高亮 */
:deep(.asm-register) {
  color: #9cdcfe;
}
/* 指令高亮 */
:deep(.asm-instruction) {
  color: #569cd6;
}
/* 标签高亮 */
:deep(.asm-label) {
  color: #dcdcaa;
}
/* 常量高亮 */
:deep(.asm-constant) {
  color: #b5cea8;
}
/* 内存寻址高亮 */
:deep(.asm-memory) {
  color: #ffd700;
}
</style>