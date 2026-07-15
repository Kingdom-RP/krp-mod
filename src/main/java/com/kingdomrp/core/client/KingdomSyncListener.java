package com.kingdomrp.core.client;

/** Экран, который надо пересобрать при приходе {@code SyncKingdomInfoPacket}. */
public interface KingdomSyncListener {
    void onKingdomSync();
}
