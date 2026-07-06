package com.kingdomrp.core.data.entry;

import com.kingdomrp.core.data.type.*;

public record CraftEntry(
        Path path,
        Spec spec,
        float xpReward      // опыт за крафт (крафт всегда успешен)
) {}
