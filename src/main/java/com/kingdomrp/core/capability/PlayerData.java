package com.kingdomrp.core.capability;

import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.data.type.Path;
import com.kingdomrp.core.data.type.SpecializationRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;

public class PlayerData implements INBTSerializable<CompoundTag> {

    public static final int MAX_SPEC_LEVEL = 10;

    private final float[] pathXP    = new float[Path.values().length];
    private final int[]   pathLevel = new int[Path.values().length];

    private final Map<String, Integer> specializationLevels = new HashMap<>();

    public int getMaxLevel(Path path) {
        return SpecializationRegistry.getCount(path) * MAX_SPEC_LEVEL;
    }

    public float getXPRequired(Path path) {
        return (float) (KRPConfig.BASE_XP.get()
                * Math.pow(KRPConfig.XP_CURVE.get(), pathLevel[path.index]));
    }

    public boolean addXP(Path path, float amount) {
        if (pathLevel[path.index] >= getMaxLevel(path)) return false;

        pathXP[path.index] += amount;
        if (pathXP[path.index] >= getXPRequired(path)) {
            pathXP[path.index] -= getXPRequired(path);
            pathLevel[path.index]++;
            return true;
        }
        return false;
    }

    public float getXPMultiplier(Path path) {
        int higherCount = 0;
        for (Path other : Path.values()) {
            if (other != path && pathLevel[other.index] > pathLevel[path.index]) {
                higherCount++;
            }
        }
        int priority = higherCount + 1;
        // Штраф по приоритету пути: 1-й = 100%, 2-й = 75%, 3-й = 50%, остальные = 33%
        return switch (priority) {
            case 1 -> 1.0f;
            case 2 -> 0.75f;
            case 3 -> 0.5f;
            default -> 0.33f;
        };
    }

    public boolean canAffordSpecialization(Path path, String specId) {
        return (pathLevel[path.index] - getTotalSpentInPath(path)) >= 1;
    }

    public boolean hasAvailablePoints(Path path) {
        return (pathLevel[path.index] - getTotalSpentInPath(path)) > 0;
    }

    public void levelUpSpecialization(String specId) {
        int current = specializationLevels.getOrDefault(specId, 0);
        if (current >= MAX_SPEC_LEVEL) return;
        specializationLevels.put(specId, current + 1);
    }

    /** Прямая установка уровня специализации в обход "очков пути" — для тестовых команд. */
    public void setSpecializationLevel(String specId, int level) {
        int clamped = Math.max(0, Math.min(MAX_SPEC_LEVEL, level));
        specializationLevels.put(specId, clamped);
    }

    public int getSpecializationLevel(String specId) {
        return specializationLevels.getOrDefault(specId, 0);
    }

    public int getTotalSpentInPath(Path path) {
        return specializationLevels.entrySet().stream()
                .filter(e -> {
                    var spec = SpecializationRegistry.get(e.getKey());
                    return spec.isPresent() && spec.get().getPathIndex() == path.index;
                })
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    public Map<String, Integer> getSpecializationLevels() {
        return specializationLevels;
    }

    public float getXP(Path path)    { return pathXP[path.index]; }
    public int   getLevel(Path path) { return pathLevel[path.index]; }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        for (Path path : Path.values()) {
            tag.putFloat("xp_" + path.index,   pathXP[path.index]);
            tag.putInt("level_" + path.index,   pathLevel[path.index]);
        }
        CompoundTag specsTag = new CompoundTag();
        specializationLevels.forEach(specsTag::putInt);
        tag.put("specializations", specsTag);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        for (Path path : Path.values()) {
            pathXP[path.index]   = tag.getFloat("xp_" + path.index);
            pathLevel[path.index] = tag.getInt("level_" + path.index);
        }
        specializationLevels.clear();
        if (tag.contains("specializations")) {
            CompoundTag specsTag = tag.getCompound("specializations");
            for (String key : specsTag.getAllKeys()) {
                specializationLevels.put(key, specsTag.getInt(key));
            }
        }
    }
}