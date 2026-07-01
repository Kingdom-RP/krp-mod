package com.kingdomrp.core.compat;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin-плагин для {@code kingdomrpcore.dynamictrees.mixins.json}: применяет
 * DT-мизины ТОЛЬКО если Dynamic Trees загружен (иначе целевой класс отсутствует).
 */
public class DTMixinPlugin implements IMixinConfigPlugin {

    private boolean dtPresent;

    @Override
    public void onLoad(String mixinPackage) {
        dtPresent = LoadingModList.get() != null
                && LoadingModList.get().getModFileById("dynamictrees") != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return dtPresent;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
