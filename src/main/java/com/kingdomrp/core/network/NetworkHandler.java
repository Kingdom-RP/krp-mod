package com.kingdomrp.core.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    // Вызывается из RegisterPayloadHandlersEvent (шина мода).
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(SyncPlayerDataPacket.TYPE, SyncPlayerDataPacket.STREAM_CODEC,
                SyncPlayerDataPacket::handle);
        registrar.playToClient(XPGainPacket.TYPE, XPGainPacket.STREAM_CODEC,
                XPGainPacket::handle);
        registrar.playToServer(ChooseSpecializationPacket.TYPE, ChooseSpecializationPacket.STREAM_CODEC,
                ChooseSpecializationPacket::handle);

        // Проверка модов клиента (config-фаза). Пэйлоады обязательные (не optional) —
        // ванильные/не-Neo клиенты отсекаются хендшейком ещё до проверки.
        registrar.configurationToClient(ModCheckRequestPayload.TYPE, ModCheckRequestPayload.STREAM_CODEC,
                ModCheckRequestPayload::handle);
        registrar.configurationToServer(ModListReplyPayload.TYPE, ModListReplyPayload.STREAM_CODEC,
                ModListReplyPayload::handle);
    }
}
