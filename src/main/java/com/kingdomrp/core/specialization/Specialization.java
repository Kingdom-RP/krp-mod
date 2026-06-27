package com.kingdomrp.core.specialization;

import com.kingdomrp.core.data.Path;

public record Specialization(String id, String name, String description, Path path) {

    public int getPathIndex() { return path.index; }
}