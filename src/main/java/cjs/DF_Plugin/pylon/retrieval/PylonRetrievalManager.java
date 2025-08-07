package cjs.DF_Plugin.pylon.retrieval;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.pylon.beaconinteraction.PylonItemListener;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class PylonRetrievalManager {
    private final DF_Main plugin;
    private static final String PREFIX = PluginUtils.colorize("&b[파일런] &f");

    public PylonRetrievalManager(DF_Main plugin) {
        this.plugin = plugin;
    }

    public void retrievePylon(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null || !clan.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(PREFIX + "§c파일런 회수는 가문 대표만 가능합니다.");
            return;
        }

        if (clan.getPylonLocations().isEmpty()) {
            player.sendMessage(PREFIX + "§c회수할 파일런이 없습니다.");
            return;
        }

        long cooldownMillis = TimeUnit.HOURS.toMillis(plugin.getPylonManager().getConfigManager().getRetrievalCooldownHours());
        if (System.currentTimeMillis() - clan.getLastRetrievalTime() < cooldownMillis) {
            long remainingMillis = cooldownMillis - (System.currentTimeMillis() - clan.getLastRetrievalTime());
            String remainingTime = String.format("%02d시간 %02d분",
                    TimeUnit.MILLISECONDS.toHours(remainingMillis),
                    TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60);
            player.sendMessage(PREFIX + "§c다음 회수까지 " + remainingTime + " 남았습니다.");
            return;
        }

        String pylonLocStr = clan.getPylonLocations().iterator().next();
        Location pylonLoc = PluginUtils.deserializeLocation(pylonLocStr);

        if (pylonLoc != null && pylonLoc.getBlock().getType() == Material.BEACON) pylonLoc.getBlock().setType(Material.AIR);

        clan.getPylonLocations().remove(pylonLocStr);
        clan.setLastRetrievalTime(System.currentTimeMillis());
        plugin.getClanManager().getStorageManager().saveClan(clan);

        player.getInventory().addItem(PylonItemListener.createPylonItem());
        player.sendMessage(PREFIX + "§a파일런을 회수했습니다.");

        plugin.getPylonManager().getReinstallManager().startReinstallTimer(player);
        player.closeInventory();
    }
}