package com.kingdomrp.core.registry;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.capability.PlayerData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Хранилище данных игрока через систему Data Attachments (1.21 заменила
 * capability). {@link PlayerData} прикрепляется к каждому игроку, сериализуется
 * в NBT и копируется при возрождении ({@code copyOnDeath}). Доступ:
 * {@code player.getData(KRPAttachments.PLAYER_DATA)} (всегда не-null, создаётся
 * лениво), запись — {@code player.setData(...)} (для примитивов мутация на месте
 * + ручной sync-пакет тоже достаточны).
 */
public class KRPAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, KingdomRPCore.MODID);

    public static final Supplier<AttachmentType<PlayerData>> PLAYER_DATA =
            ATTACHMENTS.register("player_data", () ->
                    AttachmentType.serializable(PlayerData::new).copyOnDeath().build());

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
