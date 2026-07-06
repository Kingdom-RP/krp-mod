package com.kingdomrp.core.data.map.xp;

import com.kingdomrp.core.data.map.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;
import com.kingdomrp.core.data.map.tier.*;
import com.kingdomrp.core.data.map.xp.*;

import com.kingdomrp.core.data.type.*;
import com.kingdomrp.core.data.entry.*;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
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

    public static float get(ItemStack stack) {
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
