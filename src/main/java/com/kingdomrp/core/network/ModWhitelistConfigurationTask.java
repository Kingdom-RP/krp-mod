package com.kingdomrp.core.network;

import com.kingdomrp.core.KingdomRPCore;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;

import java.util.function.Consumer;

/**
 * Задача config-фазы: проверка модов клиента по белому списку.
 * <p>
 * Регистрируется на сервере для каждого подключения через
 * {@code RegisterConfigurationTasksEvent} (см. {@link com.kingdomrp.core.system.ModWhitelist}).
 * При запуске шлёт клиенту {@link ModCheckRequestPayload} и НЕ завершается сразу —
 * задачу завершает (или отклоняет соединение) обработчик ответа
 * {@link ModListReplyPayload} → {@code ModWhitelist.validateAndProceed}.
 */
public record ModWhitelistConfigurationTask() implements ICustomConfigurationTask {

    public static final Type TYPE =
            new Type(ResourceLocation.fromNamespaceAndPath(KingdomRPCore.MODID, "mod_whitelist_check"));

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        sender.accept(new ModCheckRequestPayload());
        // Намеренно НЕ зовём finishCurrentTask: ждём ответ клиента.
    }

    @Override
    public Type type() {
        return TYPE;
    }
}
