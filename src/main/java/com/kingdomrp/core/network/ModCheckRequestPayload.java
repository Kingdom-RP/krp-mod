package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.system.ModWhitelist;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Сервер → клиент (config-фаза): запрос списка модов клиента.
 * <p>
 * Часть проверки белого списка модов. Отправляется серверным
 * {@link ModWhitelistConfigurationTask} во время конфигурации соединения; клиент
 * отвечает {@link ModListReplyPayload}. Тело пустое.
 */
public record ModCheckRequestPayload() implements CustomPacketPayload {

    public static final Type<ModCheckRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "mod_check_request"));

    public static final StreamCodec<FriendlyByteBuf, ModCheckRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new ModCheckRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Выполняется на КЛИЕНТЕ: собираем свой список модов и отвечаем серверу. */
    public static void handle(ModCheckRequestPayload payload, IPayloadContext context) {
        context.reply(new ModListReplyPayload(ModWhitelist.localModIds()));
    }
}
