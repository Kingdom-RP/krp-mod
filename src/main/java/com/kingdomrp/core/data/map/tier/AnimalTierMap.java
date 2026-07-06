package com.kingdomrp.core.data.map.tier;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * Гейтинг разведения животных по уровню Фермера + XP за разведение.
 * Распределение по ценности предоставляемых ресурсов.
 */
public class AnimalTierMap {

    private static final Map<EntityType<?>, AnimalTierEntry> MAP = new HashMap<>();

    static {
        // Ур.1 — стартовые (мясо/яйца)
        animal(1, 3f, EntityType.CHICKEN, EntityType.PIG);

        // Ур.2 — дешёвое мясо + питомцы (без ценных ресурсов)
        animal(2, 4f, EntityType.RABBIT, EntityType.WOLF,
                EntityType.OCELOT, EntityType.CAT, EntityType.FOX);

        // Ур.3 — корова/овца/грибная корова (+ доение коровы, стрижка овцы)
        animal(3, 6f, EntityType.COW, EntityType.SHEEP, EntityType.MOOSHROOM);

        // Ур.4 — коза (доение), лама, пчела, лягушка, аксолотль, броненосец (скют)
        animal(4, 8f, EntityType.GOAT, EntityType.LLAMA, EntityType.BEE,
                EntityType.FROG, EntityType.AXOLOTL, EntityType.ARMADILLO);

        // Ур.5 — транспорт
        animal(5, 8f, EntityType.HORSE, EntityType.DONKEY,
                EntityType.MULE, EntityType.CAMEL);

        // Ур.6 — черепаха (скют)
        animal(6, 10f, EntityType.TURTLE);

        // Ур.7 — панда
        animal(7, 12f, EntityType.PANDA);

        // Ур.8 — премиум (Нижний мир / древние растения)
        animal(8, 15f, EntityType.HOGLIN, EntityType.STRIDER, EntityType.SNIFFER);
    }

    private static void animal(int level, float xp, EntityType<?>... types) {
        for (EntityType<?> t : types) {
            MAP.put(t, new AnimalTierEntry(Spec.FARMER, level, xp));
        }
    }

    public static AnimalTierEntry get(EntityType<?> type) {
        return MAP.get(type);
    }
}
