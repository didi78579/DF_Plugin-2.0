package cjs.DF_Plugin.pylon.beacongui.shop;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.pylon.item.PylonItemFactory;
import cjs.DF_Plugin.items.UpgradeItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class PylonShopManager {

    public static final String SHOP_GUI_TITLE = "§6[파일런 상점]";
    private final DF_Main plugin;

    public PylonShopManager(DF_Main plugin) {
        this.plugin = plugin;
    }

    public void openShopGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, SHOP_GUI_TITLE);

        // 보조 파일런 코어 구매
        ItemStack coreItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta coreMeta = coreItem.getItemMeta();
        coreMeta.setDisplayName("§d보조 파일런 코어 구매");
        coreMeta.setLore(Arrays.asList("§7가격: 네더의 별 1개", "§7주 파일런을 보조하는 코어를 구매합니다."));
        coreItem.setItemMeta(coreMeta);
        gui.setItem(11, coreItem);

        // 강화석 교환
        int requiredLevels = plugin.getUpgradeSettingManager().getConfig().getInt("exchange-rates.experience.required-levels", 40);
        int gainedStones = plugin.getUpgradeSettingManager().getConfig().getInt("exchange-rates.experience.gained", 128);

        ItemStack stoneItem = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta stoneMeta = stoneItem.getItemMeta();
        stoneMeta.setDisplayName("§b강화석 교환");
        stoneMeta.setLore(Arrays.asList("§7가격: " + requiredLevels + " 레벨", "§7획득: 강화석 " + gainedStones + "개"));
        stoneItem.setItemMeta(stoneMeta);
        gui.setItem(15, stoneItem);

        player.openInventory(gui);
    }

    public void handleGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        event.setCancelled(true);

        if (clickedItem.getType() == Material.NETHER_STAR) {
            // 보조 파일런 코어 구매 로직
            handleBuyAuxiliaryCore(player);
        } else if (clickedItem.getType() == Material.AMETHYST_SHARD) {
            // 강화석 교환 로직
            handleExchangeStones(player);
        }
    }

    private void handleBuyAuxiliaryCore(Player player) {
        ItemStack netherStar = new ItemStack(Material.NETHER_STAR);
        if (player.getInventory().containsAtLeast(netherStar, 1)) {
            player.getInventory().removeItem(netherStar);
            player.getInventory().addItem(PylonItemFactory.createAuxiliaryCore());
            player.sendMessage("§a보조 파일런 코어를 구매했습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
        } else {
            player.sendMessage("§c네더의 별이 부족합니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
        player.closeInventory();
    }

    private void handleExchangeStones(Player player) {
        int requiredLevels = plugin.getUpgradeSettingManager().getConfig().getInt("exchange-rates.experience.required-levels", 40);
        int gainedStones = plugin.getUpgradeSettingManager().getConfig().getInt("exchange-rates.experience.gained", 128);

        if (player.getLevel() >= requiredLevels) {
            player.setLevel(player.getLevel() - requiredLevels);

            ItemStack stones = UpgradeItems.createUpgradeStone(gainedStones);
            player.getInventory().addItem(stones);

            player.sendMessage("§a강화석 " + gainedStones + "개를 교환했습니다.");
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        } else {
            player.sendMessage("§c레벨이 부족합니다. (필요: " + requiredLevels + " 레벨)");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
        player.closeInventory();
    }
}