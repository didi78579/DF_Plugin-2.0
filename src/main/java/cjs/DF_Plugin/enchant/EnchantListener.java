package cjs.DF_Plugin.enchant;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class EnchantListener implements Listener {

    private final DF_Main plugin;
    private final EnchantManager enchantManager;
    public static final String GUI_TITLE = "§5마법 부여";
    public static final int ITEM_SLOT = 4;

    public EnchantListener(DF_Main plugin) {
        this.plugin = plugin;
        this.enchantManager = plugin.getEnchantManager();
    }

    @EventHandler
    public void onEnchantTableInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.ENCHANTING_TABLE) {
            return;
        }
        event.setCancelled(true);
        openEnchantGUI(event.getPlayer());
    }

    private void openEnchantGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, GUI_TITLE);

        // 배경 아이템 설정
        ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < gui.getSize(); i++) {
            if (i != ITEM_SLOT) {
                gui.setItem(i, filler);
            }
        }

        // 중앙 슬롯 플레이스홀더
        ItemStack placeholder = new ItemStack(Material.BOOK);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d마법 부여");
            meta.setLore(Arrays.asList("§7이곳에 아이템을 올려두고", "§7좌클릭하여 마법을 부여하세요."));
            placeholder.setItemMeta(meta);
        }
        gui.setItem(ITEM_SLOT, placeholder);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();
        ItemStack cursorItem = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();

        event.setCancelled(true);

        if (event.getRawSlot() == ITEM_SLOT) {
            // 중앙 슬롯 클릭
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                // 손에 아이템을 들고 중앙 슬롯을 클릭 -> 아이템 넣기
                topInventory.setItem(ITEM_SLOT, cursorItem.clone());
                player.setItemOnCursor(null);
            } else if (currentItem != null && currentItem.getType() != Material.BOOK) {
                // 중앙 슬롯에 아이템이 있을 때 클릭
                if (event.isLeftClick()) { // 좌클릭: 인챈트 시도
                    enchantManager.attemptEnchant(player, currentItem);
                } else if (event.isRightClick()) { // 우클릭: 아이템 빼기
                    player.getInventory().addItem(currentItem);
                    topInventory.setItem(ITEM_SLOT, new ItemStack(Material.AIR)); // 슬롯 비우기
                    openEnchantGUI(player); // GUI 초기화
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        ItemStack item = event.getInventory().getItem(ITEM_SLOT);
        if (item != null && item.getType() != Material.BOOK && item.getType() != Material.AIR) {
            ((Player) event.getPlayer()).getInventory().addItem(item);
        }
    }
}