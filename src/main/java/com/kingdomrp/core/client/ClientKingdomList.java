package com.kingdomrp.core.client;

import com.kingdomrp.core.network.SyncKingdomListPacket.Entry;

import java.util.List;

/** Клиентский кэш списка всех королевств (обновляется {@link com.kingdomrp.core.network.SyncKingdomListPacket}). */
public final class ClientKingdomList {

    private static List<Entry> entries = List.of();

    private ClientKingdomList() {}

    public static void set(List<Entry> list) { entries = list; }
    public static List<Entry> get()          { return entries; }
}
