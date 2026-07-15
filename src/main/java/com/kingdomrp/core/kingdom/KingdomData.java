package com.kingdomrp.core.kingdom;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Авторитетное хранилище всех королевств. Живёт в world-сейве оверворлда
 * (saves/&lt;world&gt;/data/kingdomrp_kingdoms.dat) — переживает обновление jar мода.
 */
public class KingdomData extends SavedData {

    public static final String NAME = "kingdomrp_kingdoms";

    private final Map<UUID, Kingdom> kingdoms = new HashMap<>();

    public KingdomData() {}

    /** Получить хранилище оверворлда (единая точка для всего сервера). */
    public static KingdomData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(KingdomData::new, KingdomData::load), NAME);
    }

    // --- запросы ---

    public Collection<Kingdom> all() { return kingdoms.values(); }

    @Nullable
    public Kingdom byId(UUID id) { return kingdoms.get(id); }

    @Nullable
    public Kingdom byPlayer(UUID player) {
        for (Kingdom k : kingdoms.values()) if (k.isMember(player)) return k;
        return null;
    }

    @Nullable
    public Kingdom byChunk(ChunkPos pos) {
        for (Kingdom k : kingdoms.values()) if (k.owns(pos)) return k;
        return null;
    }

    @Nullable
    public Kingdom byChunk(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim, ChunkPos pos) {
        for (Kingdom k : kingdoms.values())
            if (k.getDimension().equals(dim) && k.owns(pos)) return k;
        return null;
    }

    // --- мутации (вызываются из KingdomManager, всегда setDirty) ---

    public void put(Kingdom k) {
        kingdoms.put(k.getId(), k);
        setDirty();
    }

    public void remove(UUID id) {
        if (kingdoms.remove(id) != null) setDirty();
    }

    /** Пометить грязным после мутации полей уже сохранённого Kingdom. */
    public void markDirty() { setDirty(); }

    // --- сериализация ---

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Kingdom k : kingdoms.values()) list.add(k.save());
        tag.put("kingdoms", list);
        return tag;
    }

    public static KingdomData load(CompoundTag tag, HolderLookup.Provider registries) {
        KingdomData data = new KingdomData();
        ListTag list = tag.getList("kingdoms", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Kingdom k = Kingdom.load(list.getCompound(i));
            data.kingdoms.put(k.getId(), k);
        }
        return data;
    }
}
