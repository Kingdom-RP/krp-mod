package com.kingdomrp.core.compat;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin-плагин для {@code kingdomrpcore.emi.mixins.json}: применяет клиентский
 * EMI-микстин только если EMI загружен. Иначе целевые классы отсутствуют.
 */
public class EmiMixinPlugin implements IMixinConfigPlugin {

    private boolean present;

    @Override
    public void onLoad(String mixinPackage) {
        present = LoadingModList.get() != null
                && LoadingModList.get().getModFileById("emi") != null;
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
