package com.kingdomrp.core.data.type;

public enum Spec {
    CARPENTER("carpenter"),
    BLACKSMITH("blacksmith"),
    CRAFTSMAN("craftsman"),
    FARMER("farmer"),
    FISHERMAN("fisherman"),
    COOK("cook"),
    MINER("miner"),
    LUMBERJACK("lumberjack"),
    WARRIOR("warrior"),
    ARCHER("archer"),
    ALCHEMIST("alchemist"),
    ENCHANTER("enchanter");

    public final String id;

    Spec(String id) {
        this.id = id;
    }
}