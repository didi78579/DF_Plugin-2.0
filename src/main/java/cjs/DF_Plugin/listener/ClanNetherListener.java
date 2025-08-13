package cjs.DF_Plugin.listener;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.clan.ClanManager;
import cjs.DF_Plugin.clan.nether.PortalHelper;
import cjs.DF_Plugin.pylon.beaconinteraction.PylonAreaManager;
import cjs.DF_Plugin.settings.GameConfigManager;
import cjs.DF_Plugin.world.WorldManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ClanNetherListener implements Listener {

    private final DF_Main plugin;
    private final GameConfigManager configManager;
    private final ClanManager clanManager;
    private final WorldManager worldManager;
    private final PylonAreaManager pylonAreaManager;

    public ClanNetherListener(DF_Main plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getGameConfigManager();
        this.clanManager = plugin.getClanManager();
        this.worldManager = plugin.getWorldManager();
        this.pylonAreaManager = plugin.getPylonManager().getAreaManager();
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!configManager.getConfig().getBoolean("pylon.features.clan-nether", true)) {
            return;
        }

        Player player = event.getPlayer();
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());

        if (clan == null) {
            player.sendMessage("§c가문에 소속되어 있어야 지옥에 입장할 수 있습니다.");
            event.setCancelled(true);
            return;
        }

        // 오버월드 -> 지옥
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL &&
            event.getFrom().getWorld().getEnvironment() == World.Environment.NORMAL) {

            if (!pylonAreaManager.isLocationInClanPylonArea(clan, event.getFrom())) {
                player.sendMessage("§c파일런 범위 내에 있는 지옥문만 사용할 수 있습니다.");
                event.setCancelled(true);
                return;
            }

            World clanNether = worldManager.getOrCreateClanNether(clan);
            // 올바른 목적지 Location 객체 생성
            Location from = event.getFrom();
            Location destination = new Location(clanNether, from.getX() / 8.0, from.getY(), from.getZ() / 8.0, from.getYaw(), from.getPitch());
            event.setTo(destination); // 서버가 포탈을 찾거나 생성하도록 목적지 설정

        }
        // 지옥 -> 오버월드
        else if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL &&
                 event.getFrom().getWorld().getName().equals(worldManager.getClanNetherWorldName(clan))) {

            Location mainPylonLocation = clan.getMainPylonLocationObject();
            if (mainPylonLocation == null) {
                player.sendMessage("§c가문의 파일런 코어 위치를 찾을 수 없어 오버월드로 귀환할 수 없습니다.");
                event.setCancelled(true);
                return;
            }

            // 주 파일런 위치로 목적지 설정. 서버가 주변에서 포탈을 찾거나 생성합니다.
            event.setTo(PortalHelper.findOrCreateSafePortal(mainPylonLocation, 16));
        }
    }
}