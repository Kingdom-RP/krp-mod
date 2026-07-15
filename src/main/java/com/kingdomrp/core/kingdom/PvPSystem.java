package com.kingdomrp.core.kingdom;

import com.kingdomrp.core.KingdomRPCore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PVP на территории королевства (server pvp=false; движок включаем глобально в
 * {@link KingdomEvents}, политику держим здесь).
 * <ul>
 *   <li>житель королевства бьёт НЕ-жителя на своей земле — можно (и «провоцирует» —
 *       жертва получает право отвечать);</li>
 *   <li>НЕ-житель бьёт жителя — только если был спровоцирован (его ударил житель);</li>
 *   <li>остальное (дикие чанки, свои-по-своим) — запрещено.</li>
 * </ul>
 * Флаг провокации сбрасывается при выходе из чанков этого королевства.
 * Проверка входа — только на смене чанка ({@link EntityEvent.EnteringSection}), не каждый тик.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public class PvPSystem {

    private static final Map<UUID, UUID> provoked = new HashMap<>();   // не-житель → id королевства

    @SubscribeEvent
    public static void onEnterChunk(EntityEvent.EnteringSection event) {
        if (!event.didChunkChange()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var dim = player.level().dimension();
        KingdomData data = KingdomData.get(player.server);
        ChunkPos nc = event.getNewPos().chunk();
        Kingdom knew = data.byChunk(dim, nc);
        Kingdom kold = data.byChunk(dim, event.getOldPos().chunk());

        if (knew != null && !knew.isMember(player.getUUID()) && knew != kold) {
            player.displayClientMessage(
                    Component.translatable("kingdomrp.pvp.entered").withStyle(ChatFormatting.RED), false);
        }
        // Вышел из территории спровоцировавшего королевства → сброс флага.
        UUID pk = provoked.get(player.getUUID());
        if (pk != null && (knew == null || !knew.getId().equals(pk))) provoked.remove(player.getUUID());
    }

    // HIGH — отменяем запрещённый pvp РАНЬШЕ XPSystem (NORMAL), иначе отменённый урон
    // всё равно даёт War-XP (отменённое событие не доходит до NORMAL-обработчиков).
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (victim == attacker) return;

        Kingdom kdef = KingdomData.get(victim.server)
                .byChunk(victim.level().dimension(), victim.chunkPosition());

        boolean allow = false;
        if (kdef != null) {
            boolean aMember = kdef.isMember(attacker.getUUID());
            boolean vMember = kdef.isMember(victim.getUUID());
            if (aMember && !vMember) {
                allow = true;
                provoked.put(victim.getUUID(), kdef.getId());       // не-житель получает право отвечать
            } else if (!aMember && vMember) {
                allow = kdef.getId().equals(provoked.get(attacker.getUUID()));
            }
        }
        if (!allow) event.setCanceled(true);
    }
}
