package com.kingdomrp.core.system;

import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.network.ModWhitelistConfigurationTask;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Серверная проверка модов клиента (анти-чит белый список).
 * <p>
 * Поток: при подключении сервер регистрирует {@link ModWhitelistConfigurationTask}
 * (config-фаза) → клиент присылает список своих модов → {@link #validateAndProceed}
 * сверяет с разрешённым набором и либо завершает задачу, либо кикает игрока.
 * <p>
 * Разрешённый набор = моды самого СЕРВЕРА ∪ {@code extraAllowedMods} из конфига.
 * Так совпадающий по модам клиент проходит «из коробки», а админ может добавить
 * клиентские моды (миникарта, шейдеры). Клиенты без нашего мода и ванильные
 * отсекаются ещё раньше — нашим (обязательным) сетевым каналом NeoForge.
 * <p>
 * ⚠️ Список модов сообщает сам клиент: это защита от «честных» и казуальных
 * клиентов, но не от глубоко модифицированных — обойти отчёт о модах принципиально
 * возможно. Полноценная серверная анти-чит-защита здесь не ставится целью.
 */
public final class ModWhitelist {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ModWhitelist() {}

    /** Список modId, установленных на ТЕКУЩЕЙ стороне (клиент или сервер). */
    public static List<String> localModIds() {
        return ModList.get().getMods().stream()
                .map(info -> info.getModId())
                .collect(Collectors.toList());
    }

    /** Разрешённый набор модов: моды сервера + extraAllowedMods из конфига. */
    private static Set<String> allowedMods() {
        Set<String> allowed = ModList.get().getMods().stream()
                .map(info -> info.getModId())
                .collect(Collectors.toSet());
        for (String extra : KRPConfig.MOD_WHITELIST_EXTRA.get()) {
            allowed.add(extra);
        }
        return allowed;
    }

    /** Регистрация задачи проверки на подключение (RegisterConfigurationTasksEvent, шина мода). */
    public static void onRegisterConfigurationTasks(RegisterConfigurationTasksEvent event) {
        if (!KRPConfig.MOD_CHECK_ENABLED.get()) return;
        event.register(new ModWhitelistConfigurationTask());
    }

    /** Валидация ответа клиента (выполняется на сервере в config-фазе). */
    public static void validateAndProceed(List<String> clientMods, IPayloadContext context) {
        context.enqueueWork(() -> {
            Set<String> allowed = allowedMods();
            List<String> disallowed = clientMods.stream()
                    .filter(id -> !allowed.contains(id))
                    .sorted()
                    .toList();

            if (!disallowed.isEmpty()) {
                LOGGER.info("[Kingdom RP] Отклонено подключение: запрещённые моды {}", disallowed);
                context.disconnect(Component.literal(
                        "§c[Kingdom RP] Подключение отклонено.\n\n"
                                + "§fНа сервере запрещены моды вне белого списка:\n"
                                + "§e" + String.join(", ", disallowed)
                                + "\n\n§7Удалите их и переподключитесь."));
                return;
            }

            context.finishCurrentTask(ModWhitelistConfigurationTask.TYPE);
        });
    }
}
