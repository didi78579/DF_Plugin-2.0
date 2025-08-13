package cjs.DF_Plugin.listener;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {

    private final DF_Main plugin;

    public PlayerChatListener(DF_Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || !plugin.getGameConfigManager().isClanChatPrefixEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());

        if (clan != null) {
            String prefix = clan.getColor() + "[" + clan.getName() + "]§r ";
            event.setFormat(prefix + event.getFormat());
        }
    }
}