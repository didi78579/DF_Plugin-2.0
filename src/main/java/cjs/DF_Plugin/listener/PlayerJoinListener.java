package cjs.DF_Plugin.listener;

import cjs.DF_Plugin.clan.ClanManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final ClanManager clanManager;

    public PlayerJoinListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // A small delay might be needed for other plugins to load player data,
        // but Bukkit's event priority usually handles this.
        clanManager.getPlayerTagManager().updatePlayerTag(player);
    }
}