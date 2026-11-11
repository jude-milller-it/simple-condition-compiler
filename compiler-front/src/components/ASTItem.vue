<template>
  <div class="tree-node">
    <div 
      class="node-wrapper" 
      :class="{ 'has-children': node.children && node.children.length > 0 }"
    >
      <div 
        class="node-content" 
        :class="node.children?.length ? 'non-terminal' : 'terminal'"
        :style="{ '--line-color': getLineColor(depth) }"
      >
        <span class="node-name">{{ node.name }}</span>
        <span v-if="node.value && node.value !== node.name" class="node-value">
          ({{ node.value }})
        </span>
      </div>
    </div>

    <div v-if="node.children && node.children.length" class="children-list">
      <div 
        v-for="(child, i) in node.children" 
        :key="i" 
        class="child-branch"
        :style="{ '--line-color': getLineColor(depth) }"
      >
        <ASTItem :node="child" :depth="depth + 1" />
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps(['node', 'depth']);

// 根据深度生成颜色逻辑
// 深度每增加 1，色相旋转 40 度
const getLineColor = (depth) => {
  const hue = (depth * 40) % 360; 
  return `hsl(${hue}, 70%, 50%)`; // 饱满但不刺眼的颜色
};
</script>

<style scoped>
/* 1. 基础结构 */
.tree-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 0 0 auto;
}

.node-wrapper {
  padding: 10px 15px;
  position: relative;
  display: flex;
  justify-content: center;
}

/* 2. 节点样式：边框颜色跟随变量 */
.node-content {
  padding: 6px 12px;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  white-space: nowrap;
  position: relative;
  z-index: 2;
  font-size: 14px;
  /* 使用动态生成的颜色 */
  border: 2px solid var(--line-color) !important;
  color: var(--line-color) !important;
}

.node-name { font-weight: bold; }
.node-value { margin-left: 4px; font-size: 12px; opacity: 0.8; font-style: italic; }

/* 3. 连线逻辑：连线颜色也跟随变量 */
.node-wrapper.has-children::after {
  content: "";
  position: absolute;
  bottom: 0; 
  left: 50%;
  width: 2px; /* 加粗一点更显色 */
  height: 10px;
  background: var(--line-color);
}

.children-list {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: flex-start;
  position: relative;
}

.child-branch {
  position: relative;
  padding-top: 15px; 
}

/* 分支竖线 */
.child-branch::before {
  content: "";
  position: absolute;
  top: 0;
  left: 50%;
  width: 2px;
  height: 15px;
  background: var(--line-color);
}

/* 分支横线 */
.child-branch::after {
  content: "";
  position: absolute;
  top: 0;
  width: 100%;
  height: 2px;
  background: var(--line-color);
}

/* 修正横线边界 */
.child-branch:first-child::after {
  left: 50%;
  width: 50%;
}
.child-branch:last-child::after {
  left: 0;
  width: 50%;
}
.child-branch:only-child::after {
  display: none;
}
</style>