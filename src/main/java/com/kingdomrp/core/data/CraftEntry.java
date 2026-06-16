package com.kingdomrp.core.data;

public record CraftEntry(
        Path path,
        Spec spec,
        float xpReward      // опыт за крафт (крафт всегда успешен)
) {}
