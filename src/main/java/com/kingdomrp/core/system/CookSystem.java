package com.kingdomrp.core.system;

import com.kingdomrp.core.registry.KRPAttachments;
import com.kingdomrp.core.config.KRPConfig;
import com.kingdomrp.core.data.FoodCookMap;
import com.kingdomrp.core.data.FoodTierEntry;
import com.kingdomrp.core.data.FoodTierMap;
import com.kingdomrp.core.data.Path;
import com.kingdomrp.core.data.Spec;
import com.kingdomrp.core.specialization.Specialization;
import com.kingdomrp.core.specialization.SpecializationRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;

/**
 * Общая логика специализации Повар (path Промысел).
 * <p>
 * Не является подписчиком событий — это библиотека хелперов, которую дёргают:
 * <ul>
 *   <li>{@link XPSystem} — гейтинг крафта еды + XP за готовку на костре;</li>
 *   <li>{@link SpecializationEffects} — гейтинг укладки сырья на костёр;</li>
 *   <li>мизины печи/коптильни — гейтинг изъятия результата + XP за готовку.</li>
 * </ul>
 */
public class CookSystem {

    /** Уровень специализации Повар у игрока (0, если не прокачан). */
    public static int cookLevel(Player player) {
        return player.getData(KRPAttachments.PLAYER_DATA).getSpecializationLevel(Spec.COOK.id);
    }

    /**
     * Может ли игрок произвести этот предмет (крафт/готовка).
     * true, если предмет не в {@link FoodTierMap} (ур.0 — без ограничений)
     * или уровень Повара достаточен.
     */
    public static boolean canProduce(Player player, Item item) {
        // Глобальный рубильник всех ограничений (как у RestrictionSystem)
        if (!KRPConfig.RESTRICTIONS_ENABLED.get()) return true;
        FoodTierEntry tier = FoodTierMap.get(item);
        if (tier == null) return true;
        return cookLevel(player) >= tier.level();
    }

    /** Сообщение об ограничении производства (троттлинг не нужен — шлём по факту попытки). */
    public static void sendRestriction(ServerPlayer player, Item item) {
        FoodTierEntry tier = FoodTierMap.get(item);
        if (tier == null) return;
        String specName = SpecializationRegistry.get(Spec.COOK.id)
                .map(Specialization::getName).orElse("Повар");
        player.sendSystemMessage(Component.literal(
                "§c[Kingdom RP] Чтобы готовить это блюдо, прокачайте навык «"
                        + specName + "» до " + tier.level() + " уровня."));
    }

    /**
     * Результат готовки сырья на костре, либо null, если костёр занят (нет
     * свободного слота) или у предмета нет рецепта готовки на костре.
     * Инкапсулирует ванильную проверку {@link CampfireBlockEntity#getCookableRecipe}.
     */
    public static Item campfireResult(Level level, CampfireBlockEntity campfire, ItemStack stack) {
        return campfire.getCookableRecipe(stack)
                .map(recipe -> recipe.value().getResultItem(level.registryAccess()).getItem())
                .orElse(null);
    }

    /**
     * Во что выплавится сырьё в печи (рецепт SMELTING), либо null, если рецепта
     * нет. Для еды результат одинаков в печи и коптильне, поэтому хватает запроса
     * SMELTING. Используется гейтом входного слота печи ({@code CookGatedInputSlot}).
     */
    public static Item smeltResult(Level level, ItemStack stack) {
        if (stack.isEmpty()) return null;
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING,
                        new net.minecraft.world.item.crafting.SingleRecipeInput(stack), level)
                .map(recipe -> recipe.value().getResultItem(level.registryAccess()).getItem())
                .orElse(null);
    }

    /**
     * Изъятие готового продукта из печи/коптильни (вызывается из мизина onTake)
     * или укладка сырья на костёр. Начисляет XP пути Промысел за штуку × количество.
     */
    public static void onCooked(Player player, ItemStack result) {
        if (player.level().isClientSide()) return;
        if (result.isEmpty()) return;
        float per = FoodCookMap.get(result.getItem());
        if (per <= 0f) return; // не относится к Повару
        // Access-гейт: за готовку выше своего уровня XP не даём, даже если сырьё
        // положил другой игрок или хоппер (вход печи такое не ловит).
        if (!canProduce(player, result.getItem())) return;
        XPSystem.giveXP(player, Path.HARVEST, per * result.getCount());
    }
}
