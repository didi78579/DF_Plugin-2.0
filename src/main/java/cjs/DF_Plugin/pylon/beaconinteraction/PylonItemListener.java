package cjs.DF_Plugin.pylon.beaconinteraction;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;

public class PylonItemListener implements Listener {

    private static final String PREFIX = PluginUtils.colorize("&b[파일런] &f");
    public static final NamespacedKey PYLON_ITEM_KEY = new NamespacedKey(DF_Main.getInstance(), "pylon_item");

    public static ItemStack createPylonItem() {
        ItemStack pylonItem = new ItemStack(Material.BEACON);
        ItemMeta meta = pylonItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b파일런");
            meta.setLore(Collections.singletonList("§7가문의 힘이 깃든 신호기."));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(PYLON_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            pylonItem.setItemMeta(meta);
        }
        return pylonItem;
    }

    public static boolean isPylonItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(PYLON_ITEM_KEY, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onDropPylon(PlayerDropItemEvent event) {
        if (isPylonItem(event.getItemDrop().getItemStack())) {
            Player player = event.getPlayer();
            player.sendMessage(PREFIX + "§c파일런 아이템은 버릴 수 없습니다.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onStorePylon(InventoryClickEvent event) {
        if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        if (isPylonItem(event.getCursor()) || isPylonItem(event.getCurrentItem())) {
            event.getWhoClicked().sendMessage(PREFIX + "§c파일런 아이템은 보관함에 넣을 수 없습니다.");
            event.setCancelled(true);
        }
    }
}