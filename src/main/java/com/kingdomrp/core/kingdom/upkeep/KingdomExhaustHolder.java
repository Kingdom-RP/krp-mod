package com.kingdomrp.core.kingdom.upkeep;

/** Реализуется миксином в {@code FoodData} — множитель расхода голода от баффа еды королевства. */
public interface KingdomExhaustHolder {
    void krp$setKingdomExhaust(float factor);
    float krp$getKingdomExhaust();
}
