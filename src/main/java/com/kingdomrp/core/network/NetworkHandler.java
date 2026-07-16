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
        registrar.playToClient(SyncKingdomInfoPacket.TYPE, SyncKingdomInfoPacket.STREAM_CODEC,
                SyncKingdomInfoPacket::handle);
        registrar.playToServer(ChooseSpecializationPacket.TYPE, ChooseSpecializationPacket.STREAM_CODEC,
                ChooseSpecializationPacket::handle);
        registrar.playToServer(SignCharterPacket.TYPE, SignCharterPacket.STREAM_CODEC,
                SignCharterPacket::handle);
        registrar.playToServer(CreateKingdomPacket.TYPE, CreateKingdomPacket.STREAM_CODEC,
                CreateKingdomPacket::handle);
        registrar.playToServer(SetKingdomColorPacket.TYPE, SetKingdomColorPacket.STREAM_CODEC,
                SetKingdomColorPacket::handle);
        registrar.playToServer(InvitePlayerPacket.TYPE, InvitePlayerPacket.STREAM_CODEC,
                InvitePlayerPacket::handle);
        registrar.playToServer(KickMemberPacket.TYPE, KickMemberPacket.STREAM_CODEC,
                KickMemberPacket::handle);
        registrar.playToServer(LeaveKingdomPacket.TYPE, LeaveKingdomPacket.STREAM_CODEC,
                LeaveKingdomPacket::handle);

        // Проверка модов клиента (config-фаза). Пэйлоады обязательные (не optional) —
        // ванильные/не-Neo клиенты отсекаются хендшейком ещё до проверки.
        registrar.configurationToClient(ModCheckRequestPayload.TYPE, ModCheckRequestPayload.STREAM_CODEC,
                ModCheckRequestPayload::handle);
        registrar.configurationToServer(ModListReplyPayload.TYPE, ModListReplyPayload.STREAM_CODEC,
                ModListReplyPayload::handle);
    }
}
