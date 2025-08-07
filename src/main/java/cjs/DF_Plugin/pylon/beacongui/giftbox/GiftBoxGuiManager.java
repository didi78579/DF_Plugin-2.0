package cjs.DF_Plugin.pylon.beacongui.giftbox;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.items.ItemManager;
import cjs.DF_Plugin.pylon.config.PylonConfigManager;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class GiftBoxGuiManager {
    private final DF_Main plugin;
    public static final String GIFT_GUI_TITLE = "§d[선물상자]";
    private final Random random = new Random();
    private static final String PREFIX = PluginUtils.colorize("&e[선물상자] &f");

    public GiftBoxGuiManager(DF_Main plugin) {
        this.plugin = plugin;
    }

    public void openGiftBox(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            // This case should ideally not happen if called from the pylon GUI
            return;
        }

        // Only the leader can open the gift box
        if (!clan.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(PREFIX + "§c가문 대표만 선물상자를 열 수 있습니다.");
            player.closeInventory();
            return;
        }

        PylonConfigManager config = plugin.getPylonManager().getConfigManager();

        if (!clan.isGiftBoxReady()) {
            long cooldownMillis = TimeUnit.HOURS.toMillis(config.getGiftboxCooldownHours());
            long timePassed = System.currentTimeMillis() - clan.getLastGiftBoxTime();
            long remainingMillis = cooldownMillis - timePassed;
            String remainingTime = String.format("%02d시간 %02d분",
                    TimeUnit.MILLISECONDS.toHours(remainingMillis),
                    TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60);
            player.sendMessage(PREFIX + "다음 선물이 도착하기까지 " + remainingTime + " 남았습니다.");
            player.closeInventory();
            return;
        }

        // Generate rewards
        int rewardSets = ThreadLocalRandom.current().nextInt(config.getGiftboxMinSets(), config.getGiftboxMaxSets() + 1);
        int totalAmount = rewardSets * 64;

        boolean isMagicStone = random.nextBoolean();
        ItemStack rewardItemType = isMagicStone ? ItemManager.createMagicStone() : ItemManager.createEnhancementStone();

        // Distribute rewards into the GUI
        Inventory gui = Bukkit.createInventory(null, 27, GIFT_GUI_TITLE);

        int amountLeft = totalAmount;
        while (amountLeft > 0) {
            int stackSize = Math.min(amountLeft, rewardItemType.getMaxStackSize());
            ItemStack stack = rewardItemType.clone();
            stack.setAmount(stackSize);
            gui.addItem(stack);
            amountLeft -= stackSize;
        }

        player.openInventory(gui);

        // Update clan status
        clan.setGiftBoxReady(false);
        clan.setLastGiftBoxTime(System.currentTimeMillis());
        plugin.getClanManager().getStorageManager().saveClan(clan);
    }
}