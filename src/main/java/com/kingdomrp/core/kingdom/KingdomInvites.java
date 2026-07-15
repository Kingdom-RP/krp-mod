package com.kingdomrp.core.kingdom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Приглашения в королевства (в памяти, не персист). Один активный инвайт на игрока.
 * Принять/Отклонить — кликабельный текст в чате → скрытая команда
 * {@code /krp kingdom accept|decline}.
 */
public final class KingdomInvites {

    private static final Map<UUID, UUID> inviteeToKingdom = new HashMap<>();  // invitee → kingdomId

    private KingdomInvites() {}

    public static void put(UUID invitee, UUID kingdomId) { inviteeToKingdom.put(invitee, kingdomId); }

    @Nullable
    public static UUID kingdomOf(UUID invitee) { return inviteeToKingdom.get(invitee); }

    public static boolean has(UUID invitee) { return inviteeToKingdom.containsKey(invitee); }

    @Nullable
    public static UUID remove(UUID invitee) { return inviteeToKingdom.remove(invitee); }

    /** Приглашённые в конкретное королевство (для UI). */
    public static List<UUID> forKingdom(UUID kingdomId) {
        List<UUID> out = new ArrayList<>();
        inviteeToKingdom.forEach((invitee, kid) -> { if (kid.equals(kingdomId)) out.add(invitee); });
        return out;
    }

    /** Кликабельное приглашение в чат приглашённому. */
    public static void sendInviteMessage(ServerPlayer invitee, String kingName, String kingdomName, int color) {
        Component name = Component.literal(kingdomName)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(true));
        Component accept = Component.translatable("kingdomrp.invite.accept")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/krp kingdom accept")));
        Component decline = Component.translatable("kingdomrp.invite.decline")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/krp kingdom decline")));

        invitee.sendSystemMessage(Component.translatable("kingdomrp.invite.text", kingName, name)
                .append(" ").append(accept).append(" ").append(decline));
    }
}
