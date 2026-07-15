package com.kingdomrp.core.kingdom.upkeep;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.kingdom.Kingdom;
import com.kingdomrp.core.registry.KRPAttachments;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Баффы/дебаффы содержания на жителях (складываются с чужими модификаторами —
 * трансиентные {@link AttributeModifier} с фиксированным id; XP-множитель в PlayerData;
 * голод — фактор в FoodData). Шаг = {@link Kingdom#step}.
 * <ul>
 *   <li>Еда: MAX_HEALTH += step; расход голода ×(1 − 0.05·step).</li>
 *   <li>Материалы: BLOCK_BREAK_SPEED ×(1 + 0.05·step).</li>
 *   <li>Довольствие: XP ×(1 + 0.05·step) (≥0, макс +25%); LUCK += 0.5·step.</li>
 * </ul>
 */
public final class KingdomBuffs {

    private static final ResourceLocation ID_HEALTH = rl("upkeep_food_health");
    private static final ResourceLocation ID_SPEED  = rl("upkeep_materials_speed");
    private static final ResourceLocation ID_LUCK   = rl("upkeep_prosperity_luck");

    private KingdomBuffs() {}

    private static ResourceLocation rl(String p) {
        return ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, p);
    }

    /** Пересчитать и применить баффы всем онлайн-жителям королевства. */
    public static void update(MinecraftServer server, Kingdom k) {
        for (UUID uuid : k.getMembers()) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) apply(p, k);
        }
    }

    public static void apply(ServerPlayer p, Kingdom k) {
        int food = k.step(Characteristic.FOOD);
        int mat  = k.step(Characteristic.MATERIALS);
        int pros = k.step(Characteristic.PROSPERITY);

        setMod(p, Attributes.MAX_HEALTH, ID_HEALTH, food, AttributeModifier.Operation.ADD_VALUE);
        setMod(p, Attributes.BLOCK_BREAK_SPEED, ID_SPEED, mat * 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setMod(p, Attributes.LUCK, ID_LUCK, pros * 0.5, AttributeModifier.Operation.ADD_VALUE);

        p.getData(KRPAttachments.PLAYER_DATA).setKingdomXpMultiplier(Math.max(0f, 1f + 0.05f * pros));
        if (p.getFoodData() instanceof KingdomExhaustHolder h) h.krp$setKingdomExhaust(1f - 0.05f * food);
    }

    /** Снять все баффы (выход из королевства / роспуск). */
    public static void clear(ServerPlayer p) {
        removeMod(p, Attributes.MAX_HEALTH, ID_HEALTH);
        removeMod(p, Attributes.BLOCK_BREAK_SPEED, ID_SPEED);
        removeMod(p, Attributes.LUCK, ID_LUCK);
        p.getData(KRPAttachments.PLAYER_DATA).setKingdomXpMultiplier(1f);
        if (p.getFoodData() instanceof KingdomExhaustHolder h) h.krp$setKingdomExhaust(1f);
    }

    private static void setMod(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id,
                               double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0.0) inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }

    private static void removeMod(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst != null) inst.removeModifier(id);
    }
}
