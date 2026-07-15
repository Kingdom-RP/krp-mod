package com.kingdomrp.core.client;

import com.kingdomrp.core.network.SyncKingdomInfoPacket;

/** Клиентский кэш сводки о королевстве (обновляется {@link SyncKingdomInfoPacket}). */
public final class ClientKingdomData {

    private static SyncKingdomInfoPacket current = SyncKingdomInfoPacket.NONE;

    private ClientKingdomData() {}

    public static void set(SyncKingdomInfoPacket info) { current = info; }
    public static SyncKingdomInfoPacket get()          { return current; }
    public static boolean inKingdom()                  { return current.inKingdom(); }
}
