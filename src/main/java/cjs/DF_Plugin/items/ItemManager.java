package cjs.DF_Plugin.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public final class ItemManager {

    private ItemManager() {}

    public static ItemStack createMagicStone() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b마석");
            meta.setLore(Collections.singletonList("§7강력한 마법의 힘이 깃든 조각."));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createEnhancementStone() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d강화석");
            meta.setLore(Collections.singletonList("§7장비 강화에 사용되는 신비한 돌."));
            item.setItemMeta(meta);
        }
        return item;
    }
}