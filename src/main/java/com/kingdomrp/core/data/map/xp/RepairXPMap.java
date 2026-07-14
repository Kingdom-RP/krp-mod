package com.kingdomrp.core.data.map.xp;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;

/**
 * XP Кузнеца (path {@link Path#CRAFT}) за ремонт/объединение предмета на
 * точильном камне. Зависит от материала предмета (ценность). Прочие чинимые
 * предметы (удочка, кресало, ножницы, лук, щит и т.п.) — базовый 1 XP.
 * <p>
 * Антиабуз: ремонт «съедает» второй предмет, который сам по себе уже дал XP при
 * крафте, поэтому фарм невыгоден — XP намеренно скромный.
 */
public class RepairXPMap {

    /** Датапак-оверрайд по конкретному предмету (перекрывает правила по материалу). */
    private static final java.util.Map<Item, Float> OVERRIDE = new java.util.HashMap<>();
    private static final java.util.List<java.util.Map.Entry<net.minecraft.tags.TagKey<Item>, Float>> OVERRIDE_TAGS = new java.util.ArrayList<>();

    public static void clearOverride() { OVERRIDE.clear(); OVERRIDE_TAGS.clear(); }
    public static void override(Item item, float xp) { OVERRIDE.put(item, xp); }
    public static void overrideTag(net.minecraft.tags.TagKey<Item> tag, float xp) { OVERRIDE_TAGS.add(java.util.Map.entry(tag, xp)); }

    public static float get(ItemStack stack) {
        Float o = OVERRIDE.get(stack.getItem());
        if (o != null) return o;
        for (var e : OVERRIDE_TAGS) {
            if (stack.getItem().builtInRegistryHolder().is(e.getKey())) return e.getValue();
        }
        if (stack.getItem() instanceof TieredItem tiered) {
            var tier = tiered.getTier();
            if (tier == Tiers.NETHERITE) return 4f;
            if (tier == Tiers.DIAMOND) return 3f;
            if (tier == Tiers.IRON) return 2f;
            return 1f; // дерево/камень/золото (+ модовые низкие тиры)
        }
        if (stack.getItem() instanceof ArmorItem armor) {
            var mat = armor.getMaterial();
            if (mat == ArmorMaterials.NETHERITE) return 4f;
            if (mat == ArmorMaterials.DIAMOND) return 3f;
            if (mat == ArmorMaterials.IRON || mat == ArmorMaterials.CHAIN) return 2f;
            return 1f; // кожа/золото/черепаха
        }
        return 1f; // удочка/кресало/ножницы/лук/щит/элитра и т.п.
    }
}
