package com.kingdomrp.core.data.map.xp;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobKillMap {

    private static final Map<EntityType<?>, KillEntry> MAP = new HashMap<>();
    /** Датапак-оверрайд (перекрывает BASE, перезагружается на /reload). */
    private static final Map<EntityType<?>, KillEntry> OVERRIDE = new HashMap<>();
    private static final List<Map.Entry<TagKey<EntityType<?>>, KillEntry>> OVERRIDE_TAGS = new ArrayList<>();

    public static void clearOverride() { OVERRIDE.clear(); OVERRIDE_TAGS.clear(); }
    public static void override(EntityType<?> type, KillEntry entry) { OVERRIDE.put(type, entry); }
    public static void overrideTag(TagKey<EntityType<?>> tag, KillEntry entry) { OVERRIDE_TAGS.add(Map.entry(tag, entry)); }

    static {
        // S+ тир
        register(new KillEntry(Path.WAR, 500f),
                EntityType.WARDEN);

        // S тир
        register(new KillEntry(Path.WAR, 300f),
                EntityType.WITHER, EntityType.ENDER_DRAGON,
                EntityType.PIGLIN_BRUTE, EntityType.RAVAGER);

        // A тир
        register(new KillEntry(Path.WAR, 75f),
                EntityType.EVOKER, EntityType.VEX,
                EntityType.ELDER_GUARDIAN, EntityType.VINDICATOR,
                EntityType.WITHER_SKELETON, EntityType.IRON_GOLEM);

        // B тир
        register(new KillEntry(Path.WAR, 15f),
                EntityType.GHAST, EntityType.SHULKER,
                EntityType.BLAZE, EntityType.HOGLIN,
                EntityType.ZOGLIN, EntityType.ENDERMAN,
                EntityType.SKELETON, EntityType.STRAY,
                EntityType.DROWNED, EntityType.CAVE_SPIDER,
                EntityType.MAGMA_CUBE,
                EntityType.BREEZE, EntityType.BOGGED); // 1.21

        // C тир
        register(new KillEntry(Path.WAR, 5f),
                EntityType.ZOMBIE, EntityType.HUSK,
                EntityType.SPIDER, EntityType.SLIME,
                EntityType.PHANTOM, EntityType.PILLAGER,
                EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN,
                EntityType.CREEPER);

        // D тир — только враждебные членистоногие (пассивные животные → Промысел)
        register(new KillEntry(Path.WAR, 1f),
                EntityType.SILVERFISH, EntityType.ENDERMITE);

        // ================================================================
        // ПУТЬ: ПРОМЫСЕЛ — Фермер (убийство животных = добыча пропитания)
        // ================================================================

        // Наземные животные — 3 XP
        register(new KillEntry(Path.HARVEST, 3f),
                EntityType.COW, EntityType.MOOSHROOM,
                EntityType.PIG, EntityType.SHEEP,
                EntityType.CHICKEN, EntityType.RABBIT,
                EntityType.TURTLE, EntityType.FOX,
                EntityType.PANDA, EntityType.POLAR_BEAR,
                EntityType.GOAT, EntityType.ARMADILLO,
                EntityType.WOLF, EntityType.LLAMA);

        // Водные животные + пчела — 5 XP
        register(new KillEntry(Path.HARVEST, 5f),
                EntityType.BEE, EntityType.SQUID,
                EntityType.GLOW_SQUID, EntityType.COD,
                EntityType.SALMON, EntityType.TROPICAL_FISH,
                EntityType.PUFFERFISH, EntityType.AXOLOTL,
                EntityType.FROG, EntityType.TADPOLE);
    }

    private static void register(KillEntry entry, EntityType<?>... types) {
        for (EntityType<?> type : types) MAP.put(type, entry);
    }

    public static KillEntry get(EntityType<?> type) {
        KillEntry o = OVERRIDE.get(type);
        if (o != null) return o;
        for (var e : OVERRIDE_TAGS) {
            if (type.builtInRegistryHolder().is(e.getKey())) return e.getValue();
        }
        return MAP.get(type);
    }
}