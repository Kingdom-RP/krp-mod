package com.kingdomrp.core.data.type;

public enum Path {
    CRAFT(0),
    HARVEST(1),
    MINING(2),
    WAR(3),
    MAGIC(4);

    public final int index;

    Path(int index) {
        this.index = index;
    }
}