package cjs.DF_Plugin.pylon.beaconinteraction;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.settings.GameConfigManager;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PylonAreaManager {
    private final DF_Main plugin;
    private final GameConfigManager configManager;
    // Map<Serialized Location, Clan>
    private final Map<String, Clan> protectedPylons = new HashMap<>();
    // Map<Serialized Pylon Location, Set<Intruder UUID>>
    private final Map<String, Set<UUID>> intruderTracker = new HashMap<>();

    public PylonAreaManager(DF_Main plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getGameConfigManager();
    }

    public void addProtectedPylon(Location location, Clan clan) {
        String locString = PluginUtils.serializeLocation(location);
        protectedPylons.put(locString, clan);
        plugin.getLogger().info("Pylon protection enabled for clan " + clan.getName() + " at " + locString);
    }

    public void removeProtectedPylon(Location location) {
        String locString = PluginUtils.serializeLocation(location);
        protectedPylons.remove(locString);
        intruderTracker.remove(locString);
        plugin.getLogger().info("Pylon protection removed at " + locString);
    }

    public void applyAreaEffects() {
        // TODO: 적 디버프 활성화 여부 설정을 config.yml로 이전 후 확인 로직 추가
//        if (!configManager.getConfig().getBoolean("pylon.enemy-debuffs-enabled", true)) {
//            if (!intruderTracker.isEmpty()) {
//                intruderTracker.clear();
//            }
//            return;
//        }

        int radius = configManager.getConfig().getInt("pylon.area-effect-radius", 50);
        int radiusSquared = radius * radius;

        Map<String, Set<UUID>> currentIntrudersByPylon = new HashMap<>();

        for (Map.Entry<String, Clan> entry : protectedPylons.entrySet()) {
            String pylonLocStr = entry.getKey();
            Clan clan = entry.getValue();
            Location pylonLoc = PluginUtils.deserializeLocation(pylonLocStr);

            if (pylonLoc == null || pylonLoc.getWorld() == null) continue;

            Set<UUID> currentIntrudersInRadius = new HashSet<>();
            Set<UUID> previouslyKnownIntruders = intruderTracker.getOrDefault(pylonLocStr, new HashSet<>());

            for (Player player : pylonLoc.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(pylonLoc) > radiusSquared) continue;

                if (!clan.getMembers().contains(player.getUniqueId())) {
                    currentIntrudersInRadius.add(player.getUniqueId());
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 0, true, true));

                    if (!previouslyKnownIntruders.contains(player.getUniqueId())) {
                        String warningMessage = PluginUtils.colorize("&c[경고] &f외부인이 파일런 영역에 접근했습니다!");
                        clan.broadcastMessage(warningMessage);
                    }
                }
            }
            currentIntrudersByPylon.put(pylonLocStr, currentIntrudersInRadius);
        }
        intruderTracker.clear();
        intruderTracker.putAll(currentIntrudersByPylon);
    }

    public boolean isLocationInClanPylonArea(Clan clan, Location location) {
        int radius = configManager.getConfig().getInt("pylon.area-effect-radius", 50);
        int radiusSquared = radius * radius;

        return clan.getPylonLocations().stream().anyMatch(pylonLocStr -> {
            Location pylonLoc = PluginUtils.deserializeLocation(pylonLocStr);
            // 월드가 같고, 거리가 반경 이내인지 확인
            return pylonLoc != null && pylonLoc.getWorld() != null && pylonLoc.getWorld().equals(location.getWorld()) && pylonLoc.distanceSquared(location) <= radiusSquared;
        });
    }
}