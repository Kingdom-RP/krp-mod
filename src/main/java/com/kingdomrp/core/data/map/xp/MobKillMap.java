package com.kingdomrp.core.data.map.xp;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.world.entity.EntityType;
import java.util.HashMap;
import java.util.Map;

public class MobKillMap {

    private static final Map<EntityType<?>, KillEntry> MAP = new HashMap<>();

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

        // D тир
        register(new KillEntry(Path.WAR, 1f),
                EntityType.SILVERFISH, EntityType.ENDERMITE,
                EntityType.WOLF, EntityType.LLAMA,
                EntityType.BEE, EntityType.POLAR_BEAR,
                EntityType.GOAT);

        // Промысел — животные
        register(new KillEntry(Path.HARVEST, 3f),
                EntityType.COW, EntityType.MOOSHROOM,
                EntityType.PIG, EntityType.SHEEP,
                EntityType.CHICKEN, EntityType.RABBIT,
                EntityType.TURTLE, EntityType.FOX,
                EntityType.PANDA);

        register(new KillEntry(Path.HARVEST, 5f),
                EntityType.BEE, EntityType.SQUID,
                EntityType.GLOW_SQUID, EntityType.COD,
                EntityType.SALMON, EntityType.TROPICAL_FISH,
                EntityType.PUFFERFISH, EntityType.AXOLOTL,
                EntityType.FROG, EntityType.TADPOLE);

        // ================================================================
        // ПУТЬ: ПРОМЫСЕЛ — Фермер (животные)
        // ================================================================

        register(new KillEntry(Path.HARVEST, 3f),
                EntityType.COW, EntityType.MOOSHROOM,
                EntityType.PIG, EntityType.SHEEP,
                EntityType.CHICKEN, EntityType.RABBIT,
                EntityType.TURTLE, EntityType.FOX,
                EntityType.PANDA, EntityType.POLAR_BEAR,
                EntityType.GOAT, EntityType.ARMADILLO); // armadillo 1.20.5

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
        return MAP.get(type);
    }
}