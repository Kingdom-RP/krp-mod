package com.kingdomrp.core.compat;

import com.kingdomrp.core.KingdomRPCore;
import com.kingdomrp.core.data.BlockXPMap;
import com.kingdomrp.core.data.Path;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Общая точка регистрации контента сторонних модов в системе прокачки по ID
 * (XP/гейт), когда для этого не нужен свой мод-специфичный класс/микстин.
 * Регистрация по строковому ID через реестр — no-op, если блока/предмета/мода нет,
 * поэтому безопасно для любого набора модов.
 * <p>
 * Сейчас: опыт Шахтёру за добычу новых натуральных камней биом-модов (Biomes We've
 * Gone, Biomes O' Plenty) — {@link BlockXPMap}, path Добыча. Цветы этих модов покрыты
 * тегами {@code minecraft:small_flowers}/{@code tall_flowers} прямо в {@link BlockXPMap}
 * (Алхимик) и здесь не дублируются. Новые интеграции по ID добавляются сюда же.
 * <p>
 * {@code PlacedBlockTracker} (в {@code XPSystem.onBlockBreak}) исключает поставленные
 * игроком блоки автоматически.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public final class ModContentCompat {

    private ModContentCompat() {}

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModContentCompat::register);
    }

    private static void register() {
        registerMiningStones();
    }

    /** Новые натуральные камни биом-модов → Шахтёр (как ванильный камень). */
    private static void registerMiningStones() {
        // Biomes We've Gone
        stone("biomeswevegone:dacite");
        stone("biomeswevegone:white_dacite");
        stone("biomeswevegone:dacite_cobblestone");
        stone("biomeswevegone:white_dacite_cobblestone");
        stone("biomeswevegone:red_rock");
        stone("biomeswevegone:rocky_stone");
        stone("biomeswevegone:overgrown_stone");

        // Biomes O' Plenty
        BlockXPMap.addById("biomesoplenty:brimstone", Path.MINING, 1.5f); // нижний, тверже
        stone("biomesoplenty:algal_end_stone");
    }

    private static void stone(String id) {
        BlockXPMap.addById(id, Path.MINING, 1f);
    }
}
