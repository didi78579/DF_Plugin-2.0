package cjs.DF_Plugin.listener;

import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.clan.ClanManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {

    private final ClanManager clanManager;

    public PlayerChatListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());

        if (clan != null) {
            String prefix = clan.getColor() + "[" + clan.getName() + "]§r ";
            event.setFormat(prefix + event.getFormat());
        }
    }
}