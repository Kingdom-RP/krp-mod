package com.kingdomrp.core.kingdom.upkeep;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Ценность предметов для характеристик содержания. */
public final class UpkeepValues {

    // Хлеб = 1 единица: nutrition 5 + saturationPoints (5·0.6·2 = 6) = 11.
    private static final float BREAD_VALUE = 11f;

    private static final TagKey<Item> C_STONES = tag("c", "stones");
    private static final TagKey<Item> C_COBBLESTONES = tag("c", "cobblestones");
    private static final TagKey<Item> C_GEMS = tag("c", "gems");
    private static final TagKey<Item> C_FOOD_POISONING = tag("c", "foods/food_poisoning");

    private UpkeepValues() {}

    private static TagKey<Item> tag(String ns, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ns, path));
    }

    /** Еда: (nutrition + saturationPoints) / хлеб. */
    public static float foodValue(ItemStack stack) {
        FoodProperties fp = stack.get(DataComponents.FOOD);
        if (fp == null) return 0f;
        if (stack.is(C_FOOD_POISONING)) return 0f;   // отравляющая еда — не ресурс
        float satPoints = fp.nutrition() * fp.saturation() * 2f;
        return (fp.nutrition() + satPoints) / BREAD_VALUE;
    }

    /** Материалы: дерево 0.5, камень 0.2. */
    public static float materialValue(ItemStack stack) {
        if (stack.is(ItemTags.LOGS)) return 0.5f;
        if (stack.is(C_STONES) || stack.is(C_COBBLESTONES)) return 0.2f;
        return 0f;
    }

    /** Довольствие: изумруд 1, алмаз 3, прочие c:gems 0.5. */
    public static float prosperityValue(ItemStack stack) {
        if (stack.is(Items.EMERALD)) return 1f;
        if (stack.is(Items.DIAMOND)) return 3f;
        if (stack.is(C_GEMS)) return 0.5f;   // lapis / quartz / amethyst_shard / prismarine_crystals
        return 0f;
    }
}
