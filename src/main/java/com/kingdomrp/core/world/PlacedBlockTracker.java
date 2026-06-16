package com.kingdomrp.core.world;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

public class PlacedBlockTracker {

    private static final Set<Long> placedByPlayer = new HashSet<>();

    public static void onPlaced(BlockPos pos) {
        placedByPlayer.add(pos.asLong());
    }

    public static void onBroken(BlockPos pos) {
        placedByPlayer.remove(pos.asLong());
    }

    public static boolean isPlacedByPlayer(BlockPos pos) {
        return placedByPlayer.contains(pos.asLong());
    }
}