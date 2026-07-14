package com.kingdomrp.core.data.map.xp;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobDamageMap {

    private static final Map<EntityType<?>, Float> MAP = new HashMap<>();
    /** Датапак-оверрайд (перекрывает BASE, перезагружается на /reload). */
    private static final Map<EntityType<?>, Float> OVERRIDE = new HashMap<>();
    private static final List<Map.Entry<TagKey<EntityType<?>>, Float>> OVERRIDE_TAGS = new ArrayList<>();

    public static void clearOverride() { OVERRIDE.clear(); OVERRIDE_TAGS.clear(); }
    public static void override(EntityType<?> type, float xp) { OVERRIDE.put(type, xp); }
    public static void overrideTag(TagKey<EntityType<?>> tag, float xp) { OVERRIDE_TAGS.add(Map.entry(tag, xp)); }

    /** Эффективные записи (BASE+override) для экспорта датапака. */
    public static Map<EntityType<?>, Float> exportEntries() {
        Map<EntityType<?>, Float> m = new HashMap<>(MAP);
        m.putAll(OVERRIDE);
        return m;
    }

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
                EntityType.MAGMA_CUBE,
                EntityType.BREEZE, EntityType.BOGGED); // 1.21

        // C тир
        register(1f,
                EntityType.ZOMBIE, EntityType.HUSK,
                EntityType.SPIDER, EntityType.SLIME,
                EntityType.PHANTOM, EntityType.PILLAGER,
                EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN,
                EntityType.CREEPER);

        // D тир — только враждебные (пассивные животные дают опыт Промыслу на убийстве,
        // урон по ним боевого опыта не даёт)
        register(0.2f,
                EntityType.SILVERFISH, EntityType.ENDERMITE);
    }

    private static void register(float xp, EntityType<?>... types) {
        for (EntityType<?> type : types) MAP.put(type, xp);
    }

    // Возвращает XP за нанесение урона сущности
    public static float get(LivingEntity entity) {
        Float o = OVERRIDE.get(entity.getType());
        if (o != null) return o;
        for (var e : OVERRIDE_TAGS) {
            if (entity.getType().builtInRegistryHolder().is(e.getKey())) return e.getValue();
        }
        if (entity instanceof ArmorStand) return 0.05f;
        return MAP.getOrDefault(entity.getType(), 0f);
    }
}