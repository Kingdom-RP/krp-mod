package com.kingdomrp.core.system;

import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.data.type.Spec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Библиотека хелперов и констант Зачарователя (path Магия, spec
 * {@link Spec#ENCHANTER}). Не подписчик событий — дёргается из мизинов стола
 * ({@code EnchantmentMenuMixin}), наковальни ({@code AnvilMenuMixin}) и
 * точильного камня ({@code GrindstoneMenuMixin}).
 * <p>
 * Логика гейтинга/шанса повторяет подход Алхимика ({@link MagicSystem}): шанс
 * успеха зависит от «запаса» уровня над требуемым тиром, XP даётся И при успехе,
 * И при провале действия (но НЕ при access-гейте).
 */
public final class EnchantSystem {

    private EnchantSystem() {}

    // Тиры по типу действия (комбинируются с тиром редкости из EnchantTierMap)
    public static final int BOOK_TABLE_LEVEL = 3; // зачарование книги на столе
    public static final int ANVIL_LEVEL      = 5; // применение книги на наковальне

    // Активные бонусы (линейно от уровня)
    public static final float LAPIS_SAVE_PER_LEVEL    = 0.05f; // ур.10 = 50%
    public static final float XP_SAVE_PER_LEVEL       = 0.05f; // ур.10 = 50%
    public static final float ENCHANT_BOOST_PER_LEVEL = 0.03f; // ур.10 = 30%
    public static final float ANVIL_DISCOUNT_PER_LEVEL = 0.05f; // ур.10 = 50% скидка
    public static final int   ANVIL_IGNORE_COST_LEVEL  = 8;     // игнор «Too Expensive»

    // Точильный камень: доля XP от снятых чар
    public static final float GRINDSTONE_XP_FRACTION = 0.5f;

    /**
     * Уровень Зачарователя, активный во время генерации чар на столе. Ставится
     * вокруг {@code getEnchantmentList} ({@code EnchantmentMenuMixin}); читается
     * фильтром пула ({@code EnchantmentHelperMixin}). {@code null} — генерация
     * НЕ со стола (лут/жители/рыбалка) → не фильтруем.
     */
    private static final ThreadLocal<Integer> TABLE_LEVEL = new ThreadLocal<>();

    public static void setTableLevel(int level) { TABLE_LEVEL.set(level); }
    public static void clearTableLevel()        { TABLE_LEVEL.remove(); }
    public static Integer tableLevel()          { return TABLE_LEVEL.get(); }

    public static int getEnchanterLevel(Player player) {
        return player.getData(KRPAttachments.PLAYER_DATA).getSpecializationLevel(Spec.ENCHANTER.id);
    }

    public static boolean restrictionsEnabled() {
        return KRPConfig.RESTRICTIONS_ENABLED.get();
    }


    public static void msg(Player player, String text) {
        player.sendSystemMessage(Component.literal(text));
    }
}
