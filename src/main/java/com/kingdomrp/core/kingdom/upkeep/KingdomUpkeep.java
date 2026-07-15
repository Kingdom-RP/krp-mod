package com.kingdomrp.core.kingdom.upkeep;

import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.kingdom.Kingdom;
import com.kingdomrp.core.kingdom.KingdomData;
import com.kingdomrp.core.kingdom.KingdomManager;
import com.kingdomrp.core.kingdom.KingdomSync;
import com.kingdomrp.core.kingdom.block.KingdomBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/** Логика содержания: поглощение ресурсов из слотов + дневное потребление. */
public final class KingdomUpkeep {

    private static final long TICKS_PER_DAY = 24000L;

    private KingdomUpkeep() {}

    public static final long PERIOD = TICKS_PER_DAY;   // 20 мин = 1 «день»

    /** Дневное потребление всех королевств (глобальный периодический вызов). */
    public static void consumeAll(MinecraftServer server) {
        KingdomData data = KingdomData.get(server);
        for (Kingdom k : new java.util.ArrayList<>(data.all())) consumeOne(server, k);
        data.markDirty();
    }

    /** Потребление одного королевства + доливка из буфера + баффы + синк. true = роспуск. */
    public static boolean consumeOne(MinecraftServer server, Kingdom k) {
        boolean disbanded = consume(server, k);
        if (!disbanded) {
            absorbAt(server, k);                 // консум освободил место — долить из буфера
            KingdomBuffs.update(server, k);
            KingdomSync.broadcast(server, k);
        }
        return disbanded;
    }

    /** Поглотить из слотов блока, если он загружен. */
    public static void absorbAt(MinecraftServer server, Kingdom k) {
        ServerLevel level = server.getLevel(k.getDimension());
        if (level == null || !level.isLoaded(k.getBlockPos())) return;
        if (level.getBlockEntity(k.getBlockPos()) instanceof KingdomBlockEntity be) absorb(be, k);
    }

    /** Переливание из слотов в характеристики, пока есть место (≥ ценности предмета). */
    public static void absorb(KingdomBlockEntity be, Kingdom k) {
        SimpleContainer res = be.getResources();
        for (Characteristic c : Characteristic.VALUES) {
            ItemStack s = res.getItem(c.index);
            if (s.isEmpty()) continue;
            float v = c.value(s);
            if (v <= 0f) continue;
            boolean changed = false;
            while (!s.isEmpty() && k.getCharacteristic(c) + v <= Characteristic.MAX) {
                k.addCharacteristic(c, v);
                s.shrink(1);
                changed = true;
            }
            if (changed) {
                if (s.isEmpty()) res.setItem(c.index, ItemStack.EMPTY);
                res.setChanged();
            }
        }
    }

    /** Дневное потребление. true = какая-то характеристика достигла 0 → роспуск. */
    private static boolean consume(MinecraftServer server, Kingdom k) {
        int residents = k.getMembers().size();
        int chunks = k.getClaims().size();
        int levels = k.sumMemberLevels();

        k.addCharacteristic(Characteristic.FOOD,
                -(float) (residents * KRPConfig.UPKEEP_FOOD_PER_RESIDENT.get()));
        k.addCharacteristic(Characteristic.MATERIALS,
                -(float) (chunks * KRPConfig.UPKEEP_MATERIALS_PER_CHUNK.get()));
        k.addCharacteristic(Characteristic.PROSPERITY,
                -(float) (levels * KRPConfig.UPKEEP_PROSPERITY_PER_LEVEL.get()));

        for (Characteristic c : Characteristic.VALUES) {
            if (k.getCharacteristic(c) <= 0f) {
                KingdomManager.disband(server, k);
                return true;
            }
        }
        return false;
    }
}
