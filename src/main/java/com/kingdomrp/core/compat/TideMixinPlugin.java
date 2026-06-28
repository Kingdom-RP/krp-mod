package com.kingdomrp.core.compat;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin-плагин для конфига {@code kingdomrpcore.tide.mixins.json}: применяет
 * Tide-мизины ТОЛЬКО если мод Tide загружен. Иначе целевой класс отсутствует и
 * мизин бы упал. Проверка через {@link LoadingModList} (доступна на фазе микстинов).
 */
public class TideMixinPlugin implements IMixinConfigPlugin {

    private boolean tidePresent;

    @Override
    public void onLoad(String mixinPackage) {
        tidePresent = LoadingModList.get() != null
                && LoadingModList.get().getModFileById("tide") != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return tidePresent;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
