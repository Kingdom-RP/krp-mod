package com.kingdomrp.core.specialization;

import com.kingdomrp.core.data.Path;

public class Specialization {

    private final String id;
    private final String name;
    private final String description;
    private final Path path;

    public Specialization(String id, String name, String description, Path path) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.path = path;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Path getPath() { return path; }
    public int getPathIndex() { return path.index; } // оставляем для обратной совместимости
}