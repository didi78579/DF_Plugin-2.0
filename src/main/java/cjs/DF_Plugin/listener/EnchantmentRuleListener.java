package cjs.DF_Plugin.listener;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.settings.GameConfigManager;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class EnchantmentRuleListener implements Listener {

    private final GameConfigManager configManager;

    public EnchantmentRuleListener(DF_Main plugin) {
        this.configManager = plugin.getGameConfigManager();
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        boolean breachDisabled = !configManager.getConfig().getBoolean("op-enchant.breach-enabled", false);
        boolean thornsDisabled = !configManager.getConfig().getBoolean("op-enchant.thorns-enabled", false);

        if (breachDisabled) {
            event.getEnchantsToAdd().remove(Enchantment.BREACH);
        }
        if (thornsDisabled) {
            event.getEnchantsToAdd().remove(Enchantment.THORNS);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null || !result.hasItemMeta()) return;

        boolean breachDisabled = !configManager.getConfig().getBoolean("op-enchant.breach-enabled", false);
        boolean thornsDisabled = !configManager.getConfig().getBoolean("op-enchant.thorns-enabled", false);

        if (breachDisabled && hasEnchantment(result, Enchantment.BREACH)) {
            event.setResult(null);
        }
        if (thornsDisabled && hasEnchantment(result, Enchantment.THORNS)) {
            event.setResult(null);
        }
    }

    private boolean hasEnchantment(ItemStack item, Enchantment enchantment) {
        if (item.getEnchantments().containsKey(enchantment)) return true;
        if (item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            return meta.getStoredEnchants().containsKey(enchantment);
        }
        return false;
    }
}