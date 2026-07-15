package com.kingdomrp.core.kingdom.upkeep;

import com.kingdomrp.core.KingdomRPCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Периодическое потребление содержания — один глобальный обработчик (без по-блочного тикера).
 * Раз в {@link KingdomUpkeep#PERIOD} тиков монотонного времени проходим по всем королевствам.
 * Поглощение ресурсов — событийно (листенер слотов в BE), здесь только консум + доливка.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public class KingdomUpkeepEvents {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.overworld().getGameTime() % KingdomUpkeep.PERIOD == 0L)
            KingdomUpkeep.consumeAll(server);
    }
}
