package cjs.DF_Plugin.pylon.beaconinteraction.registration;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BeaconRegistrationManager {
    private final DF_Main plugin;
    private static final String PREFIX = PluginUtils.colorize("&b[파일런] &f");

    public BeaconRegistrationManager(DF_Main plugin) {
        this.plugin = plugin;
    }

    public void registerPylon(Player player, Block beacon, Clan clan) {
        // 1. 철 블록 기반 설치
        placeIronBase(beacon.getLocation());

        // 2. 수직 배리어 설치
        placeVerticalBarrier(beacon.getLocation());

        // 3. 파일런 정보 등록
        String locationString = PluginUtils.serializeLocation(beacon.getLocation());
        clan.addPylonLocation(locationString);
        clan.setLastGiftBoxTime(System.currentTimeMillis()); // 선물상자 타이머 시작
        plugin.getClanManager().getStorageManager().saveClan(clan);

        // 재설치 타이머가 있다면 취소
        plugin.getPylonManager().getReinstallManager().cancelReinstallTimer(player);

        // 4. 영역 보호 활성화 (PylonAreaManager에게 알림)
        plugin.getPylonManager().getAreaManager().addProtectedPylon(beacon.getLocation(), clan);

        player.sendMessage(PREFIX + "파일런이 성공적으로 설치 및 활성화되었습니다!");
    }

    private void placeIronBase(Location beaconLocation) {
        Location baseCenter = beaconLocation.clone().subtract(0, 1, 0);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                baseCenter.clone().add(x, 0, z).getBlock().setType(Material.IRON_BLOCK);
            }
        }
    }

    private void placeVerticalBarrier(Location beaconLocation) {
        World world = beaconLocation.getWorld();
        if (world == null) return;
        for (int y = beaconLocation.getBlockY() + 1; y < world.getMaxHeight(); y++) {
            new Location(world, beaconLocation.getX(), y, beaconLocation.getZ()).getBlock().setType(Material.BARRIER);
        }
    }
}