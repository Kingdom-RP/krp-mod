package com.kingdomrp.core.compat;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin-плагин для конфига {@code kingdomrpcore.backpacks.mixins.json}: применяет
 * мизин ТОЛЬКО если мод Backpacks загружен (иначе целевой класс отсутствует →
 * мизин упал бы). Проверка через {@link LoadingModList} (доступна на фазе микстинов).
 */
public class BackpacksMixinPlugin implements IMixinConfigPlugin {

    private boolean present;

    @Override
    public void onLoad(String mixinPackage) {
        present = LoadingModList.get() != null
                && LoadingModList.get().getModFileById("backpacks") != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return present;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
