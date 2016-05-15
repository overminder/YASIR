package com.github.overmind.yasir.value;

import java.util.HashMap;

public interface Symbol {
    String name();

    HashMap<String, Symbol> nameMap = new HashMap<>();

    static Symbol apply(String name) {
        Symbol sym =  nameMap.get(name);
        if (sym == null) {
            sym = () -> name;
            nameMap.put(name, sym);
        }
        return sym;
    }
}
