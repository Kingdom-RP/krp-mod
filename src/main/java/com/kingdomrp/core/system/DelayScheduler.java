package com.kingdomrp.core.system;

import com.kingdomrp.core.KingdomRPCore;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Простой планировщик отложенных серверных задач (на N тиков вперёд).
 * <p>
 * {@code MinecraftServer.tell(TickTask)} НЕ даёт задержку: {@code shouldRun} =
 * {@code getTick()+3 < tickCount || haveTime()} → при наличии свободного времени
 * задача выполняется в тот же тик. Поэтому ведём свою очередь и разбираем её в
 * {@link ServerTickEvent.Post}. Работает только на серверном потоке.
 */
@EventBusSubscriber(modid = KingdomRPCore.MODID)
public final class DelayScheduler {

    private record Task(long fireTick, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();

    private DelayScheduler() {}

    /** Выполнить {@code action} через {@code delayTicks} тиков (серверный поток). */
    public static void schedule(MinecraftServer server, int delayTicks, Runnable action) {
        TASKS.add(new Task(server.getTickCount() + Math.max(1, delayTicks), action));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (TASKS.isEmpty()) return;
        long now = event.getServer().getTickCount();
        // Снимок готовых задач (action может ставить новые — не мешаем итерации).
        List<Runnable> ready = null;
        for (var it = TASKS.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (t.fireTick() <= now) {
                if (ready == null) ready = new ArrayList<>();
                ready.add(t.action());
                it.remove();
            }
        }
        if (ready != null) for (Runnable r : ready) r.run();
    }
}
