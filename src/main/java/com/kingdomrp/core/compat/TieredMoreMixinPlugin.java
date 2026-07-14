package com.kingdomrp.core.compat;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin-плагин для {@code kingdomrpcore.tieredmore.mixins.json}: применяет мизины
 * ТОЛЬКО если мод Tiered More загружен (иначе цель-класс отсутствует → мизин упал бы).
 */
public class TieredMoreMixinPlugin implements IMixinConfigPlugin {

    private boolean present;

    @Override
    public void onLoad(String mixinPackage) {
        present = LoadingModList.get() != null
                && LoadingModList.get().getModFileById("tiered_more") != null;
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
