package cjs.DF_Plugin.pylon.beaconinteraction;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.settings.GameConfigManager;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Particle;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.UUID;

public class PylonAreaManager {
    private final DF_Main plugin;
    private final GameConfigManager configManager;
    // Map<Serialized Location, Clan>
    private final Map<String, Clan> protectedPylons = new HashMap<>();
    // Map<Serialized Pylon Location, Set<Intruder UUID>>
    private final Map<String, Set<UUID>> intruderTracker = new HashMap<>();
    private BukkitTask particleTask;

    public PylonAreaManager(DF_Main plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getGameConfigManager();
        startParticleTask();
    }

    public void addProtectedPylon(Location location, Clan clan) {
        String locString = PluginUtils.serializeLocation(location);
        protectedPylons.put(locString, clan);
        plugin.getLogger().info("Pylon protection enabled for clan " + clan.getName() + " at " + locString);
    }

    public void removeProtectedPylon(Location location) {
        String locString = PluginUtils.serializeLocation(location);
        protectedPylons.remove(locString);
        if (protectedPylons.isEmpty() && particleTask != null) {
            particleTask.cancel();
        }
        intruderTracker.remove(locString);
        plugin.getLogger().info("Pylon protection removed at " + locString);
    }

    /**
     * 지정된 위치에 파일런이 있는지 확인하고, 있다면 해당 파일런을 소유한 가문을 반환합니다.
     * @param location 확인할 위치
     * @return 파일런을 소유한 Clan 객체, 없으면 null
     */
    public Clan getClanAt(Location location) {
        Location blockLoc = location.getBlock().getLocation();
        for (Entry<String, Clan> entry : protectedPylons.entrySet()) {
            Location pylonLoc = PluginUtils.deserializeLocation(entry.getKey());
            if (pylonLoc != null && pylonLoc.equals(blockLoc)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 지정된 위치의 블록이 파일런의 보호받는 구조물(기반, 배리어)의 일부인지 확인합니다.
     * @param location 확인할 위치
     * @return 구조물의 일부이면 true
     */
    public boolean isPylonStructureBlock(Location location) {
        for (String pylonLocStr : protectedPylons.keySet()) {
            Location pylonLoc = PluginUtils.deserializeLocation(pylonLocStr);
            if (pylonLoc == null || !pylonLoc.getWorld().equals(location.getWorld())) {
                continue;
            }

            // 기반 블록(y-1의 3x3 영역)인지 확인
            if (location.getBlockY() == pylonLoc.getBlockY() - 1) {
                if (Math.abs(location.getBlockX() - pylonLoc.getBlockX()) <= 1 &&
                    Math.abs(location.getBlockZ() - pylonLoc.getBlockZ()) <= 1) {
                    return true;
                }
            }
            // 배리어 블록(비콘 위 수직 기둥)인지 확인
            if (location.getBlockX() == pylonLoc.getBlockX() && location.getBlockZ() == pylonLoc.getBlockZ() && location.getBlockY() > pylonLoc.getBlockY()) {
                return true;
            }
        }
        return false;
    }

    public void applyAreaEffects() {
        boolean allyBuffEnabled = configManager.isPylonAllyBuffEnabled();
        boolean enemyDebuffEnabled = configManager.isPylonEnemyDebuffEnabled();

        if (!allyBuffEnabled && !enemyDebuffEnabled) {
            if (!intruderTracker.isEmpty()) {
                intruderTracker.clear();
            }
            return;
        }

        int radius = configManager.getConfig().getInt("pylon.area-effects.radius", 50);
        int radiusSquared = radius * radius;

        // 아군에게 적용할 효과
        final PotionEffect allySaturation = new PotionEffect(PotionEffectType.SATURATION, 120, 0, true, true);
        final PotionEffect allyHaste = new PotionEffect(PotionEffectType.HASTE, 120, 1, true, true); // 성급함 2
        // 적군에게 적용할 효과
        final PotionEffect enemySlowness = new PotionEffect(PotionEffectType.SLOWNESS, 120, 1, true, true); // 구속 2
        final PotionEffect enemyFatigue = new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 1, true, true); // 채굴 피로 2
        final PotionEffect enemyGlowing = new PotionEffect(PotionEffectType.GLOWING, 120, 0, true, true); // 발광

        Map<String, Set<UUID>> currentIntrudersByPylon = new HashMap<>();

        for (Map.Entry<String, Clan> entry : protectedPylons.entrySet()) {
            String pylonLocStr = entry.getKey();
            Clan clan = entry.getValue();
            Location pylonLoc = PluginUtils.deserializeLocation(pylonLocStr);

            if (pylonLoc == null || pylonLoc.getWorld() == null) continue;

            Set<UUID> currentIntrudersInRadius = new HashSet<>();
            Set<UUID> previouslyKnownIntruders = intruderTracker.getOrDefault(pylonLocStr, new HashSet<>());

            for (Player player : pylonLoc.getWorld().getPlayers()) {
                // Y축을 무시한 2D 거리 계산
                if (distanceSquared2D(player.getLocation(), pylonLoc) > radiusSquared) continue;

                if (clan.getMembers().contains(player.getUniqueId())) {
                    // 아군일 경우: 버프 적용
                    if (allyBuffEnabled) {
                        player.addPotionEffect(allySaturation);
                        player.addPotionEffect(allyHaste);
                    }
                } else {
                    // 적군일 경우: 디버프 적용 및 경고
                    if (enemyDebuffEnabled) {
                        currentIntrudersInRadius.add(player.getUniqueId());
                        player.addPotionEffect(enemySlowness);
                        player.addPotionEffect(enemyFatigue);
                        player.addPotionEffect(enemyGlowing);

                        if (!previouslyKnownIntruders.contains(player.getUniqueId())) {
                            String warningMessage = PluginUtils.colorize("&c[경고] &f외부인이 파일런 영역에 접근했습니다!");
                            clan.broadcastMessage(warningMessage);
                        }
                    }
                }
            }
            if (enemyDebuffEnabled) {
                currentIntrudersByPylon.put(pylonLocStr, currentIntrudersInRadius);
            }
        }

        if (enemyDebuffEnabled) {
            intruderTracker.clear();
            intruderTracker.putAll(currentIntrudersByPylon);
        } else {
            intruderTracker.clear();
        }
    }

    public boolean isLocationInClanPylonArea(Clan clan, Location location) {
        int radius = configManager.getConfig().getInt("pylon.area-effects.radius", 50);
        int radiusSquared = radius * radius;

        return clan.getPylonLocations().stream().anyMatch(pylonLocStr -> {
            Location pylonLoc = PluginUtils.deserializeLocation(pylonLocStr);
            // 월드가 같고, Y축을 무시한 2D 거리가 반경 이내인지 확인
            return pylonLoc != null && pylonLoc.getWorld() != null && pylonLoc.getWorld().equals(location.getWorld()) && distanceSquared2D(location, pylonLoc) <= radiusSquared;
        });
    }

    /**
     * 두 위치의 Y축을 무시한 2D 거리의 제곱을 계산합니다.
     * @param loc1 첫 번째 위치
     * @param loc2 두 번째 위치
     * @return 2D 거리의 제곱
     */
    private double distanceSquared2D(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null || !loc1.getWorld().equals(loc2.getWorld())) {
            return Double.MAX_VALUE;
        }
        double dx = loc1.getX() - loc2.getX();
        double dz = loc1.getZ() - loc2.getZ();
        return dx * dx + dz * dz;
    }

    /**
     * 파일런 보호 구역의 외곽선에 파티클을 생성하는 작업을 시작합니다.
     */
    public void startParticleTask() {
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (protectedPylons.isEmpty()) return;
                // Use a copy to prevent ConcurrentModificationException if pylons are added/removed
                new HashMap<>(protectedPylons).forEach((locStr, clan) -> {
                    Location pylonLoc = PluginUtils.deserializeLocation(locStr);
                    if (pylonLoc != null) {
                        spawnBoundaryParticles(pylonLoc);
                    }
                });
            }
        }.runTaskTimer(plugin, 0L, 40L); // 2초마다 실행
    }

    private void spawnBoundaryParticles(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        int radius = configManager.getConfig().getInt("pylon.area-effects.radius", 50);
        final int points = 75; // 파티클 밀도 (고정값)
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            spawnParticleAtTop(world, (int) x, (int) z);
        }
    }

    private void spawnParticleAtTop(World world, int x, int z) {
        Block highestBlock = world.getHighestBlockAt(x, z);
        Location particleLoc = highestBlock.getLocation().add(0.5, 1.2, 0.5); // 블록 중앙, 약간 위
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0, 0, 0, 0); // 파티클 종류 (고정값)
    }
}