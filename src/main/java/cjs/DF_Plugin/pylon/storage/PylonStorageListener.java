package cjs.DF_Plugin.pylon.storage;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class PylonStorageListener implements Listener {

    private final PylonStorageManager storageManager;
    private final DF_Main plugin;

    public PylonStorageListener(DF_Main plugin, PylonStorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
    }

    @EventHandler
    public void onStorageClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(PylonStorageManager.STORAGE_TITLE)) {
            Player player = (Player) event.getPlayer();
            Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
            if (clan != null) {
                // 인벤토리가 닫힐 때 파일에 저장합니다.
                storageManager.saveStorage(clan);
            }
        }
    }
}