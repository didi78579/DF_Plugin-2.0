package cjs.DF_Plugin.upgrade;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.items.UpgradeItems;
import cjs.DF_Plugin.upgrade.gui.UpgradeGUI;
import cjs.DF_Plugin.upgrade.profile.WeaponProfileManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;
import java.util.Set;

public class UpgradeListener implements Listener {

    private final DF_Main plugin;
    private final UpgradeManager upgradeManager;
    private final WeaponProfileManager weaponProfileManager;
    private final UpgradeGUI upgradeGUI;

    private static final Set<Material> ANVIL_TYPES = Set.of(
            Material.ANVIL,
            Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL
    );

    public UpgradeListener(DF_Main plugin) {
        this.plugin = plugin;
        this.upgradeManager = plugin.getUpgradeManager();
        this.weaponProfileManager = plugin.getWeaponProfileManager();
        this.upgradeGUI = new UpgradeGUI(plugin);
    }

    @EventHandler
    public void onAnvilClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !ANVIL_TYPES.contains(clickedBlock.getType())) return;

        event.setCancelled(true);
        upgradeGUI.open(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(UpgradeGUI.GUI_TITLE)) {
            return;
        }

        event.setCancelled(true); // 기본 동작 방지

        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInventory = event.getView().getTopInventory();
        int slot = event.getRawSlot();

        // GUI 내부 클릭만 처리
        if (slot < topInventory.getSize()) {
            switch (slot) {
                case UpgradeGUI.BUY_DIAMOND_SLOT -> handleBuyWithDiamonds(player);
                case UpgradeGUI.BUY_XP_SLOT -> handleBuyWithXP(player);
                case UpgradeGUI.UPGRADE_ITEM_SLOT -> {
                    if (event.isLeftClick()) {
                        handleUpgrade(player, topInventory);
                    } else if (event.isRightClick()) {
                        handleWithdraw(player, topInventory);
                    }
                }
            }
        }
        // 플레이어 인벤토리에서 아이템을 GUI로 옮기는 경우
        else if (event.getClickedInventory() instanceof PlayerInventory) {
            handlePlaceItem(event, player, topInventory);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(UpgradeGUI.GUI_TITLE)) return;

        ItemStack item = event.getInventory().getItem(UpgradeGUI.UPGRADE_ITEM_SLOT);
        // 플레이스홀더가 아닌 실제 아이템만 돌려줌
        if (item != null && !item.isSimilar(UpgradeGUI.createAnvilPlaceholder())) {
            giveOrDropItems((Player) event.getPlayer(), item);
        }
    }

    private void handleBuyWithDiamonds(Player player) {
        FileConfiguration config = plugin.getUpgradeSettingManager().getConfig();
        int required = config.getInt("exchange-rates.diamond.required", 1);
        int gained = config.getInt("exchange-rates.diamond.gained", 1);

        if (player.getInventory().contains(Material.DIAMOND, required)) {
            player.getInventory().removeItem(new ItemStack(Material.DIAMOND, required));
            player.sendMessage(ChatColor.AQUA + "다이아몬드로 강화석을 구매하셨습니다!");
            giveOrDropItems(player, UpgradeItems.createUpgradeStone(gained));
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.8f);
            player.sendMessage(ChatColor.RED + "다이아몬드가 부족합니다!");
        }
    }

    private void handleBuyWithXP(Player player) {
        FileConfiguration config = plugin.getUpgradeSettingManager().getConfig();
        int requiredLevels = config.getInt("exchange-rates.experience.required-levels", 40);
        int gainedStones = config.getInt("exchange-rates.experience.gained", 128);

        if (player.getLevel() >= requiredLevels) {
            player.setLevel(player.getLevel() - requiredLevels);
            player.sendMessage(ChatColor.GREEN + "경험치로 강화석을 구매하셨습니다!");
            giveOrDropItems(player, UpgradeItems.createUpgradeStone(gainedStones));
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.8f);
            player.sendMessage(ChatColor.RED + "경험치가 부족합니다!");
        }
    }

    private void handleUpgrade(Player player, Inventory inventory) {
        ItemStack targetItem = inventory.getItem(UpgradeGUI.UPGRADE_ITEM_SLOT);

        if (targetItem == null || targetItem.isSimilar(UpgradeGUI.createAnvilPlaceholder())) {
            player.sendMessage(ChatColor.RED + "강화할 아이템을 올려주세요.");
            return;
        }

        upgradeManager.attemptUpgrade(player, targetItem);

        if (targetItem.getAmount() == 0) {
            inventory.setItem(UpgradeGUI.UPGRADE_ITEM_SLOT, UpgradeGUI.createAnvilPlaceholder());
        }
    }

    private void handleWithdraw(Player player, Inventory inventory) {
        ItemStack targetItem = inventory.getItem(UpgradeGUI.UPGRADE_ITEM_SLOT);

        if (targetItem != null && !targetItem.isSimilar(UpgradeGUI.createAnvilPlaceholder())) {
            giveOrDropItems(player, targetItem);
            inventory.setItem(UpgradeGUI.UPGRADE_ITEM_SLOT, UpgradeGUI.createAnvilPlaceholder());
        }
    }

    private void handlePlaceItem(InventoryClickEvent event, Player player, Inventory inventory) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemStack itemInSlot = inventory.getItem(UpgradeGUI.UPGRADE_ITEM_SLOT);
        if (itemInSlot != null && itemInSlot.isSimilar(UpgradeGUI.createAnvilPlaceholder())) {
            if (weaponProfileManager.getProfile(clickedItem.getType()) != null) {
                inventory.setItem(UpgradeGUI.UPGRADE_ITEM_SLOT, clickedItem.clone());
                event.setCurrentItem(null);
            } else {
                player.sendMessage(ChatColor.RED + "이 아이템은 강화할 수 없습니다.");
            }
        }
    }

    private void giveOrDropItems(Player player, ItemStack items) {
        Map<Integer, ItemStack> notAdded = player.getInventory().addItem(items);
        if (!notAdded.isEmpty()) {
            Location loc = player.getLocation();
            notAdded.values().forEach(item -> loc.getWorld().dropItem(loc, item));
            player.sendMessage(ChatColor.YELLOW + "인벤토리가 가득 차 아이템이 바닥에 드롭되었습니다.");
        }
    }
}