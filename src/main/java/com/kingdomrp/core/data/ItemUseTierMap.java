package com.kingdomrp.core.data;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Гейтинг НОШЕНИЯ/ИСПОЛЬЗОВАНИЯ предмета по уровню специализации.
 * Броня и ближнее оружие — по уровню Воина (а не пути Война целиком), арбалет —
 * по уровню Лучника. Одно требование на предмет (носить умеет один спек).
 * <p>
 * Крафт гейтится отдельно ({@link ItemCraftTierMap}).
 */
public class ItemUseTierMap {

    private static final Map<Item, SpecRequirement> MAP = new HashMap<>();

    static {
        // Воин — броня и ближнее оружие
        register(new SpecRequirement(Spec.WARRIOR, 1),
                Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE,
                Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);

        register(new SpecRequirement(Spec.WARRIOR, 2),
                Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE,
                Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
                Items.SHIELD, Items.TURTLE_HELMET);

        register(new SpecRequirement(Spec.WARRIOR, 3),
                Items.IRON_HELMET, Items.IRON_CHESTPLATE,
                Items.IRON_LEGGINGS, Items.IRON_BOOTS,
                Items.IRON_SWORD);

        register(new SpecRequirement(Spec.WARRIOR, 4),
                Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE,
                Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);

        register(new SpecRequirement(Spec.WARRIOR, 5),
                Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
                Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
                Items.DIAMOND_SWORD, Items.DIAMOND_AXE);

        register(new SpecRequirement(Spec.WARRIOR, 7),
                Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE,
                Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
                Items.NETHERITE_SWORD, Items.NETHERITE_AXE,
                Items.MACE); // булава 1.21 — мощное ближнее оружие из Trial Chambers

        // Лучник — арбалет
        register(new SpecRequirement(Spec.ARCHER, 3),
                Items.CROSSBOW);
    }

    private static void register(SpecRequirement req, Item... items) {
        for (Item item : items) MAP.put(item, req);
    }

    public static SpecRequirement get(Item item) {
        return MAP.get(item);
    }

    /** Регистрация гейта использования по ID (мод-совместимость). No-op если предмета нет. */
    public static void addById(String id, SpecRequirement req) {
        net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(net.minecraft.resources.ResourceLocation.parse(id))
                .ifPresent(it -> MAP.put(it, req));
    }
}
