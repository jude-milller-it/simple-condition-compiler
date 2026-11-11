package com.sky.compilerbackend.util;

import com.sky.compilerbackend.pojo.Symbol;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collection;

@Component
public class SymbolTable {
    // 使用 LinkedHashMap 保持插入顺序，方便查看
    private final Map<String, Symbol> symbols = new LinkedHashMap<>();
    private int nextAddress = 0; // 模拟虚拟地址分配器

    public void addSymbol(String name, String type, String scope) {
        if (!symbols.containsKey(name)) {
            Symbol symbol = new Symbol(name, type, nextAddress, scope);
            symbols.put(name, symbol);
            nextAddress += 4; // 假设每个变量占4个字节
        }
    }

    public boolean contains(String name) { return symbols.containsKey(name); }

    public void clear() {
        symbols.clear();
        nextAddress = 0;
    }

    public Collection<Symbol> getAllSymbols() { return symbols.values(); }
}