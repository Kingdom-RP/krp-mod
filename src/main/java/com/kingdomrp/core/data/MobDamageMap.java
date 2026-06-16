package com.kingdomrp.core.data;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.LivingEntity;
import java.util.HashMap;
import java.util.Map;

public class MobDamageMap {

    private static final Map<EntityType<?>, Float> MAP = new HashMap<>();

    static {
        // S+ тир
        register(3f, EntityType.WARDEN);

        // S тир
        register(3f,
                EntityType.WITHER, EntityType.ENDER_DRAGON,
                EntityType.PIGLIN_BRUTE, EntityType.RAVAGER);

        // A тир
        register(1.5f,
                EntityType.EVOKER, EntityType.VEX,
                EntityType.ELDER_GUARDIAN, EntityType.VINDICATOR,
                EntityType.WITHER_SKELETON, EntityType.IRON_GOLEM);

        // B тир
        register(1f,
                EntityType.GHAST, EntityType.SHULKER,
                EntityType.BLAZE, EntityType.HOGLIN,
                EntityType.ZOGLIN, EntityType.ENDERMAN,
                EntityType.SKELETON, EntityType.STRAY,
                EntityType.DROWNED, EntityType.CAVE_SPIDER,
                EntityType.MAGMA_CUBE);

        // C тир
        register(1f,
                EntityType.ZOMBIE, EntityType.HUSK,
                EntityType.SPIDER, EntityType.SLIME,
                EntityType.PHANTOM, EntityType.PILLAGER,
                EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN,
                EntityType.CREEPER);

        // D тир
        register(0.2f,
                EntityType.SILVERFISH, EntityType.ENDERMITE,
                EntityType.WOLF, EntityType.LLAMA,
                EntityType.BEE, EntityType.POLAR_BEAR,
                EntityType.GOAT);
    }

    private static void register(float xp, EntityType<?>... types) {
        for (EntityType<?> type : types) MAP.put(type, xp);
    }

    // Возвращает XP за нанесение урона сущности
    public static float get(LivingEntity entity) {
        if (entity instanceof ArmorStand) return 0.05f;
        return MAP.getOrDefault(entity.getType(), 0f);
    }
}