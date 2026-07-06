package com.kingdomrp.core.data.type;

public record Specialization(String id, String name, String description, Path path) {

    public int getPathIndex() { return path.index; }
}