package cjs.DF_Plugin.player.stats;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class StatsListener implements Listener {

    private final StatsManager statsManager;

    // GUI 버튼의 동작과 타입을 식별하기 위한 키
    public static final NamespacedKey STATS_ACTION_KEY = new NamespacedKey(DF_Main.getInstance(), "stats_action");
    public static final NamespacedKey STATS_TYPE_KEY = new NamespacedKey(DF_Main.getInstance(), "stats_type");

    public StatsListener(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith(StatsEditor.GUI_TITLE_PREFIX)) {
            return;
        }

        event.setCancelled(true);

        Player editor = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR || !clickedItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // 버튼이 아니면 무시
        if (!container.has(STATS_ACTION_KEY, PersistentDataType.STRING)) {
            return;
        }

        String targetName = event.getView().getTitle().replace(StatsEditor.GUI_TITLE_PREFIX, "");
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            editor.sendMessage("§c평가 대상 플레이어를 찾을 수 없습니다.");
            editor.closeInventory();
            return;
        }

        String action = container.get(STATS_ACTION_KEY, PersistentDataType.STRING);
        if (action == null) return;

        switch (action) {
            case "SAVE":
                statsManager.saveStats();
                editor.sendMessage("§a" + target.getName() + "님의 스탯을 저장했습니다.");
                editor.closeInventory();
                break;

            case "INCREMENT":
            case "DECREMENT":
                if (!container.has(STATS_TYPE_KEY, PersistentDataType.STRING)) return;
                try {
                    String typeStr = container.get(STATS_TYPE_KEY, PersistentDataType.STRING);
                    StatType type = StatType.valueOf(typeStr);
                    boolean increment = action.equals("INCREMENT");

                    statsManager.updateStatFromGUI(target, type, increment);
                    // 변경사항을 반영하여 GUI를 새로고침합니다.
                    editor.openInventory(StatsEditor.create(target, statsManager.getPlayerStats(target.getUniqueId())));
                } catch (IllegalArgumentException e) {
                    editor.sendMessage("§c내부 오류가 발생했습니다. (Invalid StatType)");
                }
                break;
        }
    }
}