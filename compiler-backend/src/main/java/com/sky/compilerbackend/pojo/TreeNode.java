package com.sky.compilerbackend.pojo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class TreeNode {
    private String name;
    private String value;
    private List<TreeNode> children = new ArrayList<>();

    public TreeNode(String name) { this.name = name; }
    public TreeNode(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * 关键修正：归约弹栈顺序是反的，
     * 每次添加子节点都要插入到列表最前面，这样最后出来的顺序才是正常的 [左, 中, 右]
     */
    public void addChild(TreeNode child) {
        if (child != null) {
            this.children.add(0, child);
        }
    }
}