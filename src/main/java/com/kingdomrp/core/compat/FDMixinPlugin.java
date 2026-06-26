package com.kingdomrp.core.compat;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin-плагин для конфига {@code kingdomrpcore.farmersdelight.mixins.json}:
 * применяет FD-мизины ТОЛЬКО если Farmer's Delight загружен. Иначе цель-класс
 * отсутствует и мизин бы упал — поэтому {@link #shouldApplyMixin} возвращает
 * false. Проверка через {@link LoadingModList} (доступна на фазе микстинов,
 * до полной инициализации FML).
 */
public class FDMixinPlugin implements IMixinConfigPlugin {

    private boolean fdPresent;

    @Override
    public void onLoad(String mixinPackage) {
        fdPresent = LoadingModList.get() != null
                && LoadingModList.get().getModFileById("farmersdelight") != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return fdPresent;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
