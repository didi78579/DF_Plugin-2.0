package cjs.DF_Plugin.pylon.beaconinteraction;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.clan.ClanManager;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class PylonProtectionListener implements Listener {

    private final DF_Main plugin;
    private final ClanManager clanManager;

    public PylonProtectionListener(DF_Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
    }

    @EventHandler
    public void onPylonBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.BEACON) return;

        String brokenLocationStr = PluginUtils.serializeLocation(block.getLocation());

        clanManager.getClanByPylonLocation(brokenLocationStr).ifPresent(clan -> {
            boolean wasMain = clan.isMainPylon(brokenLocationStr);
            clan.removePylonLocation(brokenLocationStr);
            clanManager.getStorageManager().saveClan(clan);

            String message = wasMain ? "§c주 파일런이 파괴되었습니다!" : "§c보조 파일런이 파괴되었습니다!";
            clan.broadcastMessage(message);

            event.getPlayer().sendMessage("§a" + clan.getName() + " 가문의 파일런을 파괴했습니다.");
        });
    }
}