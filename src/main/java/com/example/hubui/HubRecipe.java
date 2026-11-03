package com.example.hubui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Рецепт вкладки "Создание". Добавляется обычным JSON (датапак или ресурсы мода):
 * data/<namespace>/recipes/<имя>.json, "type": "hubui:hub_crafting". Примеры - в README.
 */
public class HubRecipe implements Recipe<Container> {
    public record Req(Ingredient ingredient, int count) {}

    private final ResourceLocation id;
    public final String category;
    public final String description;
    private final ItemStack result;
    public final List<Req> reqs;

    public HubRecipe(ResourceLocation id, String category, String description, ItemStack result, List<Req> reqs) {
        this.id = id;
        this.category = category;
        this.description = description;
        this.result = result;
        this.reqs = reqs;
    }

    @Override public boolean matches(Container c, Level level) { return false; }

    @Override public ItemStack assemble(Container c, RegistryAccess access) { return result.copy(); }

    @Override public boolean canCraftInDimensions(int w, int h) { return true; }

    @Override public ItemStack getResultItem(RegistryAccess access) { return result; }

    @Override public ResourceLocation getId() { return id; }

    @Override public RecipeSerializer<?> getSerializer() { return ModRegistry.HUB_RECIPE_SERIALIZER.get(); }

    @Override public RecipeType<?> getType() { return ModRegistry.HUB_RECIPE_TYPE.get(); }

    /** Прячет рецепт из ванильной книги рецептов. */
    @Override public boolean isSpecial() { return true; }

    public static class Serializer implements RecipeSerializer<HubRecipe> {
        @Override
        public HubRecipe fromJson(ResourceLocation id, JsonObject json) {
            String category = GsonHelper.getAsString(json, "category", "misc");
            String description = GsonHelper.getAsString(json, "description", "");
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            List<Req> reqs = new ArrayList<>();
            JsonArray arr = GsonHelper.getAsJsonArray(json, "ingredients");
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                reqs.add(new Req(Ingredient.fromJson(o), Math.max(1, GsonHelper.getAsInt(o, "count", 1))));
            }
            return new HubRecipe(id, category, description, result, reqs);
        }

        @Override
        public HubRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String category = buf.readUtf();
            String description = buf.readUtf(2000);
            ItemStack result = buf.readItem();
            int n = buf.readVarInt();
            List<Req> reqs = new ArrayList<>();
            for (int i = 0; i < n; i++) reqs.add(new Req(Ingredient.fromNetwork(buf), buf.readVarInt()));
            return new HubRecipe(id, category, description, result, reqs);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, HubRecipe r) {
            buf.writeUtf(r.category);
            buf.writeUtf(r.description, 2000);
            buf.writeItem(r.result);
            buf.writeVarInt(r.reqs.size());
            for (Req q : r.reqs) {
                q.ingredient().toNetwork(buf);
                buf.writeVarInt(q.count());
            }
        }
    }
}
