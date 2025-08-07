package cjs.DF_Plugin.misc;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.settings.GameConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

public class RecipeManager {

    private final DF_Main plugin;
    private final GameConfigManager configManager;
    private final NamespacedKey notchedAppleKey;

    public RecipeManager(DF_Main plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getGameConfigManager();
        this.notchedAppleKey = new NamespacedKey(plugin, "notched_apple");
    }

    public void updateRecipes() {
        updateNotchedAppleRecipe();
    }

    private void updateNotchedAppleRecipe() {
        boolean enabled = configManager.getConfig().getBoolean("utility.notched-apple-recipe", true);

        // 레시피가 활성화되어야 하는데, 현재 서버에 레시피가 없다면 추가
        if (enabled && Bukkit.getRecipe(notchedAppleKey) == null) {
            ShapedRecipe recipe = new ShapedRecipe(notchedAppleKey, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
            recipe.shape("GGG", "GAG", "GGG");
            recipe.setIngredient('G', Material.GOLD_BLOCK);
            recipe.setIngredient('A', Material.APPLE);
            Bukkit.addRecipe(recipe);
        } else if (!enabled && Bukkit.getRecipe(notchedAppleKey) != null) {
            Bukkit.removeRecipe(notchedAppleKey);
        }
    }
}