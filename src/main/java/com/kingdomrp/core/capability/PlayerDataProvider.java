package com.kingdomrp.core.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerDataProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<PlayerData> PLAYER_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerData data = new PlayerData();
    private final LazyOptional<PlayerData> optional = LazyOptional.of(() -> data);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_DATA ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        data.deserializeNBT(tag);
    }

    public void invalidate() {
        optional.invalidate();
    }
}