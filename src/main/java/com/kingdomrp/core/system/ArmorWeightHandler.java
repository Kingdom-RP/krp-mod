package com.kingdomrp.core.system;

import com.kingdomrp.core.KingdomRPCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Штраф скорости за ношение брони. Каждая часть режет скорость; шлем/сапоги — меньше
 * (0.2 от полного сета), нагрудник/поножи — больше (0.3). Полный сет по материалу:
 * кожа 5%, кольчуга 10%, золото/железо 15%, алмаз 20%, незерит 25%. Части из разных
 * материалов складываются корректно (значения per-piece суммируются).
 * <p>
 * Применяется одним трансиентным модификатором {@code MOVEMENT_SPEED}
 * ({@code ADD_MULTIPLIED_TOTAL}). Пересчёт: {@code LivingEquipmentChangeEvent}
 * (надел/снял/раздатчик/дроп при смерти/команды), логин и респавн (новый entity —
 * трансиент теряется).
 */
public final class ArmorWeightHandler {

    private static final ResourceLocation MOD_ID =
            ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "armor_weight");

    /** Предмет брони → доля штрафа скорости этой части. */
    private static final Map<Item, Float> PENALTY = new HashMap<>();

    static {
        add(0.05f, Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);
        add(0.10f, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);
        add(0.15f, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);
        add(0.15f, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        add(0.20f, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        add(0.25f, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
    }

    private ArmorWeightHandler() {}

    /** Шлем/сапоги = 0.2 полного сета, нагрудник/поножи = 0.3. Сумма = full. */
    private static void add(float full, Item helmet, Item chest, Item legs, Item boots) {
        PENALTY.put(helmet, full * 0.2f);
        PENALTY.put(boots,  full * 0.2f);
        PENALTY.put(chest,  full * 0.3f);
        PENALTY.put(legs,   full * 0.3f);
    }

    /** Пересчитать штраф скорости по надетой броне и переустановить модификатор. */
    public static void recompute(Player player) {
        AttributeInstance ms = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (ms == null) return;

        float total = 0f;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            total += PENALTY.getOrDefault(player.getItemBySlot(slot).getItem(), 0f);
        }

        ms.removeModifier(MOD_ID);   // idempotent
        if (total > 0f) {
            ms.addTransientModifier(new AttributeModifier(
                    MOD_ID, -total, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
