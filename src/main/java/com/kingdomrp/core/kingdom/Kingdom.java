package com.kingdomrp.core.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import com.kingdomrp.core.kingdom.upkeep.Characteristic;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Одно королевство: имя, король, участники, заклеймленные чанки, центр, измерение.
 * Авторитетное хранилище — {@link KingdomData}; FTB Teams/Chunks = зеркало.
 */
public class Kingdom {

    public static final int DATA_VERSION = 1;

    private final UUID id;
    private String name;
    private UUID king;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<Long> claims  = new HashSet<>();   // ChunkPos.toLong()
    private long center;                                 // ChunkPos.toLong()
    private BlockPos blockPos;                            // точная позиция блока королевства
    private ResourceKey<Level> dimension;
    @Nullable private UUID teamId;                        // id команды FTB Teams (зеркало)
    private int color = 0xFFFFFF;                         // цвет королевства (RGB, заливка на карте FTB)

    // Содержание (upkeep): характеристики 0..MAX, снапшот уровней жителей, день последнего потребления.
    private final float[] characteristics = {
            com.kingdomrp.core.kingdom.upkeep.Characteristic.DEFAULT,
            com.kingdomrp.core.kingdom.upkeep.Characteristic.DEFAULT,
            com.kingdomrp.core.kingdom.upkeep.Characteristic.DEFAULT };
    private final Map<UUID, Integer> memberLevels = new HashMap<>();
    private long lastConsumeDay = -1;

    public Kingdom(UUID id, String name, UUID king, ChunkPos center, BlockPos blockPos,
                   ResourceKey<Level> dimension) {
        this.id = id;
        this.name = name;
        this.king = king;
        this.center = center.toLong();
        this.blockPos = blockPos;
        this.dimension = dimension;
        this.members.add(king);
    }

    private Kingdom(UUID id) { this.id = id; }

    public UUID getId()                 { return id; }
    public String getName()             { return name; }
    public void setName(String name)    { this.name = name; }
    public UUID getKing()               { return king; }
    public void setKing(UUID king)      { this.king = king; }
    public Set<UUID> getMembers()       { return members; }
    public boolean isMember(UUID p)     { return members.contains(p); }
    public boolean isKing(UUID p)       { return king.equals(p); }
    public Set<Long> getClaims()        { return claims; }
    public boolean owns(ChunkPos pos)   { return claims.contains(pos.toLong()); }
    public ChunkPos getCenter()         { return new ChunkPos(center); }
    public BlockPos getBlockPos()       { return blockPos; }
    public ResourceKey<Level> getDimension() { return dimension; }
    @Nullable public UUID getTeamId()   { return teamId; }
    public void setTeamId(@Nullable UUID id) { this.teamId = id; }
    public int getColor()               { return color; }
    public void setColor(int color)     { this.color = color & 0xFFFFFF; }

    // --- содержание ---

    public float getCharacteristic(Characteristic c) { return characteristics[c.index]; }

    public void setCharacteristic(Characteristic c, float v) {
        characteristics[c.index] = Math.max(0f, Math.min(Characteristic.MAX, v));
    }

    public void addCharacteristic(Characteristic c, float delta) {
        setCharacteristic(c, characteristics[c.index] + delta);
    }

    /** Шаг баффа/дебаффа: (value−500)/100, кламп [−5,+5]. >0 = бафф, <0 = дебафф. */
    public int step(Characteristic c) {
        int s = Math.round((characteristics[c.index] - Characteristic.DEFAULT) / 100f);
        return Math.max(-5, Math.min(5, s));
    }

    public void setMemberLevel(UUID uuid, int total) { memberLevels.put(uuid, total); }
    public void removeMemberLevel(UUID uuid)         { memberLevels.remove(uuid); }
    public int getMemberLevel(UUID uuid)             { return memberLevels.getOrDefault(uuid, 0); }

    public int sumMemberLevels() {
        int sum = 0;
        for (int v : memberLevels.values()) sum += v;
        return sum;
    }

    public long getLastConsumeDay()          { return lastConsumeDay; }
    public void setLastConsumeDay(long day)  { this.lastConsumeDay = day; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("dataVersion", DATA_VERSION);
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putUUID("king", king);
        tag.putLong("center", center);
        tag.put("blockPos", NbtUtils.writeBlockPos(blockPos));
        tag.putString("dimension", dimension.location().toString());
        if (teamId != null) tag.putUUID("teamId", teamId);
        tag.putInt("color", color);

        tag.putFloat("food", characteristics[0]);
        tag.putFloat("materials", characteristics[1]);
        tag.putFloat("prosperity", characteristics[2]);
        tag.putLong("lastConsumeDay", lastConsumeDay);
        ListTag levels = new ListTag();
        memberLevels.forEach((uuid, lvl) -> {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", uuid);
            e.putInt("lvl", lvl);
            levels.add(e);
        });
        tag.put("memberLevels", levels);

        ListTag membersTag = new ListTag();
        for (UUID m : members) {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", m);
            membersTag.add(e);
        }
        tag.put("members", membersTag);

        long[] claimArr = claims.stream().mapToLong(Long::longValue).toArray();
        tag.put("claims", new LongArrayTag(claimArr));
        return tag;
    }

    public static Kingdom load(CompoundTag tag) {
        Kingdom k = new Kingdom(tag.getUUID("id"));
        k.name = tag.getString("name");
        k.king = tag.getUUID("king");
        k.center = tag.getLong("center");
        k.blockPos = NbtUtils.readBlockPos(tag, "blockPos").orElse(BlockPos.ZERO);
        if (tag.hasUUID("teamId")) k.teamId = tag.getUUID("teamId");
        if (tag.contains("color")) k.color = tag.getInt("color");
        if (tag.contains("food")) {
            k.characteristics[0] = tag.getFloat("food");
            k.characteristics[1] = tag.getFloat("materials");
            k.characteristics[2] = tag.getFloat("prosperity");
        }
        k.lastConsumeDay = tag.getLong("lastConsumeDay");
        ListTag levels = tag.getList("memberLevels", Tag.TAG_COMPOUND);
        for (int i = 0; i < levels.size(); i++) {
            CompoundTag e = levels.getCompound(i);
            k.memberLevels.put(e.getUUID("id"), e.getInt("lvl"));
        }
        k.dimension = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.parse(tag.getString("dimension")));

        ListTag membersTag = tag.getList("members", Tag.TAG_COMPOUND);
        for (int i = 0; i < membersTag.size(); i++) {
            k.members.add(membersTag.getCompound(i).getUUID("id"));
        }
        for (long c : tag.getLongArray("claims")) k.claims.add(c);
        return k;
    }
}
