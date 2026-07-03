package com.kingdomrp.core.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;

/**
 * EMI-плагин: показывает рецепты варки пива в Кеге AlcoCraft+ (тип
 * {@code alcocraftplus:beer_brewing}). AlcoCraft+ поставляет только JEI/REI-плагины, в
 * EMI своей категории нет — добавляем сами. Класс грузит сам EMI (по {@link EmiEntrypoint});
 * без EMI не грузится. Активен только если загружен AlcoCraft+.
 * <p>
 * Рецепты фильтруем по классу мод-рецепта (без хард-зависимости — через имя класса):
 * ингредиенты и результат берём ванильным API {@link RecipeHolder}/{@code Recipe}.
 */
@EmiEntrypoint
public class AlcoEmiPlugin implements EmiPlugin {

    private static final String ALCO = "alcocraftplus";
    private static final String KEG_RECIPE_CLASS = "me.itzme1on.alcocraftplus.core.recipes.KegRecipes";

    public static final EmiRecipeCategory KEG_CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("kingdomrpcore", "keg_brewing"),
            kegIcon());

    @Override
    public void register(EmiRegistry registry) {
        if (!ModList.get().isLoaded(ALCO)) return;

        registry.addCategory(KEG_CATEGORY);
        Item keg = item(ALCO + ":keg");
        if (keg != null) registry.addWorkstation(KEG_CATEGORY, EmiStack.of(keg));

        RegistryAccess access = registryAccess();
        if (access == null) return;

        for (RecipeHolder<?> holder : registry.getRecipeManager().getRecipes()) {
            if (!holder.value().getClass().getName().equals(KEG_RECIPE_CLASS)) continue;
            ItemStack result = holder.value().getResultItem(access);
            registry.addRecipe(new KegEmiRecipe(KEG_CATEGORY, holder, result));
        }
    }

    private static EmiStack kegIcon() {
        Item keg = item(ALCO + ":keg");
        return keg != null ? EmiStack.of(keg) : EmiStack.EMPTY;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(null);
    }

    private static RegistryAccess registryAccess() {
        return Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess() : null;
    }
}
