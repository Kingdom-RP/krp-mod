package com.kingdomrp.core.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * Одна варка в Кеге для EMI: 4 ингредиента → пиво. Ингредиенты/результат — ванильным
 * API (типы AlcoCraft+ не нужны).
 */
public class KegEmiRecipe implements EmiRecipe {

    private final EmiRecipeCategory category;
    private final ResourceLocation id;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public KegEmiRecipe(EmiRecipeCategory category, RecipeHolder<?> holder, ItemStack result) {
        this.category = category;
        this.id = holder.id();
        this.inputs = holder.value().getIngredients().stream()
                .map(EmiIngredient::of).toList();
        this.outputs = List.of(EmiStack.of(result));
    }

    @Override public EmiRecipeCategory getCategory() { return category; }
    @Override public ResourceLocation getId() { return id; }
    @Override public List<EmiIngredient> getInputs() { return inputs; }
    @Override public List<EmiStack> getOutputs() { return outputs; }
    @Override public int getDisplayWidth() { return 118; }
    @Override public int getDisplayHeight() { return 36; }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        // 4 входных слота в ряд → стрелка → результат.
        for (int i = 0; i < inputs.size(); i++) {
            widgets.addSlot(inputs.get(i), i * 18, 9);
        }
        widgets.addFillingArrow(76, 9, 4000);
        widgets.addSlot(outputs.get(0), 98, 9).recipeContext(this);
    }
}
