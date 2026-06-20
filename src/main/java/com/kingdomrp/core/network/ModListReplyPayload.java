package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.system.ModWhitelist;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Клиент → сервер (config-фаза): список modId, установленных на клиенте.
 * <p>
 * Ответ на {@link ModCheckRequestPayload}. Сервер сверяет его с белым списком
 * ({@link ModWhitelist}) и либо завершает задачу конфигурации, либо отключает
 * игрока.
 */
public record ModListReplyPayload(List<String> modIds) implements CustomPacketPayload {

    public static final Type<ModListReplyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "mod_list_reply"));

    public static final StreamCodec<FriendlyByteBuf, ModListReplyPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ModListReplyPayload::modIds,
                    ModListReplyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Выполняется на СЕРВЕРЕ (config-фаза): валидация белого списка. */
    public static void handle(ModListReplyPayload payload, IPayloadContext context) {
        ModWhitelist.validateAndProceed(payload.modIds(), context);
    }
}
