package cjs.DF_Plugin.upgrade.gui;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UpgradeGUI {

    private final DF_Main plugin;
    public static final String GUI_TITLE = "장비 대장간";
    public static final int UPGRADE_ITEM_SLOT = 4; // 중앙
    public static final int BUY_DIAMOND_SLOT = 0;
    public static final int BUY_XP_SLOT = 8;

    public UpgradeGUI(DF_Main plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        FileConfiguration config = plugin.getUpgradeSettingManager().getConfig();

        Inventory gui = Bukkit.createInventory(null, 9, GUI_TITLE);

        // 강화 모루 슬롯 초기화
        gui.setItem(UPGRADE_ITEM_SLOT, createAnvilPlaceholder());

        // 강화석 구매 슬롯 (다이아몬드)
        ItemStack diamondItem = new ItemStack(Material.DIAMOND);
        ItemMeta diamondMeta = diamondItem.getItemMeta();
        if (diamondMeta != null) {
            diamondMeta.setDisplayName(ChatColor.AQUA + "다이아몬드로 강화석 구매");
            int diamondRequired = config.getInt("exchange-rates.diamond.required", 1);
            int diamondGained = config.getInt("exchange-rates.diamond.gained", 1);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "다이아 " + diamondRequired + "개 => 강화석 " + diamondGained + "개");
            diamondMeta.setLore(lore);
            diamondItem.setItemMeta(diamondMeta);
        }
        gui.setItem(BUY_DIAMOND_SLOT, diamondItem);

        // 강화석 구매 슬롯 (경험치)
        ItemStack xpItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta xpMeta = xpItem.getItemMeta();
        if (xpMeta != null) {
            xpMeta.setDisplayName(ChatColor.GREEN + "경험치로 강화석 구매");
            int xpRequired = config.getInt("exchange-rates.experience.required-levels", 40);
            int xpGained = config.getInt("exchange-rates.experience.gained", 128);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "" + xpRequired + " 레벨 => 강화석 " + xpGained + "개");
            xpMeta.setLore(lore);
            xpItem.setItemMeta(xpMeta);
        }
        gui.setItem(BUY_XP_SLOT, xpItem);

        player.openInventory(gui);
    }

    public static ItemStack createAnvilPlaceholder() {
        ItemStack anvil = new ItemStack(Material.ANVIL);
        ItemMeta meta = anvil.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "강화 모루");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "이곳에 강화할 아이템을 올리세요."));
            anvil.setItemMeta(meta);
        }
        return anvil;
    }
}