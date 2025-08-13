package cjs.DF_Plugin.events.supplydrop;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.UpgradeManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SupplyDropManager {

    private final DF_Main plugin;
    private boolean isEventActive = false;
    private Location altarLocation;
    private final Map<Location, BlockData> originalBlocks = new HashMap<>();
    private BossBar supplyDropBossBar;
    private BukkitTask bossBarUpdateTask;
    private final Map<UUID, BukkitTask> breakingTasks = new ConcurrentHashMap<>();

    public SupplyDropManager(DF_Main plugin) {
        this.plugin = plugin;
    }

    public void triggerEvent() {
        if (isEventActive) return;

        World world = Bukkit.getWorlds().get(0);
        Location randomLocation = getRandomSafeLocation(world);
        Location groundLocation = world.getHighestBlockAt(randomLocation).getLocation();

        this.isEventActive = true;
        this.altarLocation = groundLocation.clone().add(0, 3, 0); // 알 위치를 기준으로 저장

        spawnAltar(groundLocation);

        Bukkit.broadcastMessage("§d[보급] §f월드 어딘가에 강력한 기운이 감지됩니다!");

        // 보스바 생성 및 활성화
        long delayHours = plugin.getGameConfigManager().getConfig().getInt("events.supply-drop.spawn-delay-hours", 1);
        long durationMillis = TimeUnit.HOURS.toMillis(delayHours);
        long startTime = System.currentTimeMillis();

        supplyDropBossBar = Bukkit.createBossBar("§d[보급] §f제단 봉인 해제까지...", BarColor.PURPLE, BarStyle.SOLID);
        supplyDropBossBar.setVisible(true);
        for (Player p : Bukkit.getOnlinePlayers()) {
            supplyDropBossBar.addPlayer(p);
        }

        bossBarUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= durationMillis || !isEventActive) {
                    cleanupBossBar();
                    this.cancel();
                    return;
                }

                double progress = 1.0 - ((double) elapsed / durationMillis);
                supplyDropBossBar.setProgress(Math.max(0, progress));

                long remainingSeconds = (durationMillis - elapsed) / 1000;
                String title = String.format("§d[보급] §f제단 봉인 해제까지... %d분 %d초", remainingSeconds / 60, remainingSeconds % 60);
                supplyDropBossBar.setTitle(title);
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1초마다 업데이트

        // 설정된 시간 뒤 보급 활성화
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isEventActive) {
                    Bukkit.broadcastMessage("§d[보급] §f제단의 봉인이 해제되었습니다! 알을 파괴하여 보상을 획득하세요!");
                    // 나침반 지급
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        ItemStack compass = new ItemStack(Material.COMPASS);
                        ItemMeta itemMeta = compass.getItemMeta();
                        if (itemMeta instanceof org.bukkit.inventory.meta.CompassMeta) {
                            org.bukkit.inventory.meta.CompassMeta compassMeta = (org.bukkit.inventory.meta.CompassMeta) itemMeta;
                            compassMeta.setDisplayName("§d보급 위치 나침반");
                            compassMeta.setLodestone(altarLocation);
                            compassMeta.setLodestoneTracked(true);
                            compass.setItemMeta(compassMeta);
                        }
                        player.getInventory().addItem(compass);
                    }
                }
            }
        }.runTaskLater(plugin, durationMillis / 50); // Ticks
    }

    private void spawnAltar(Location groundLocation) {
        originalBlocks.clear();
        Location baseLocation = groundLocation.clone(); // 제단 최하단

        // --- 1. 제단이 지어질 모든 위치의 '원래' 블록 데이터를 미리 저장 ---
        saveOriginalBlock(baseLocation, 1, -1, 1); // 네더라이트 층
        saveOriginalBlock(baseLocation.clone().add(0, 1, 0), 1, 0, 1); // 신호기 층
        saveOriginalBlock(baseLocation.clone().add(0, 2, 0), 1, -1, 1); // 계단 층
        saveOriginalBlock(baseLocation.clone().add(0, 3, 0), 0, 0, 0); // 알 위치

        // --- 2. 실제 제단 건설 ---
        // 네더라이트 3x3
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                baseLocation.clone().add(x, 0, z).getBlock().setType(Material.NETHERITE_BLOCK);
            }
        }

        // 신호기 층
        Location beaconFloor = baseLocation.clone().add(0, 1, 0);
        beaconFloor.getBlock().setType(Material.BEACON);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                beaconFloor.clone().add(x, 0, z).getBlock().setType(Material.PURPLE_STAINED_GLASS);
            }
        }

        // 계단 층
        Location stairFloor = baseLocation.clone().add(0, 2, 0);
        stairFloor.getBlock().setType(Material.PURPLE_STAINED_GLASS);
        // 모서리
        for (int x = -1; x <= 1; x += 2) {
            for (int z = -1; z <= 1; z += 2) {
                Block stairBlock = stairFloor.clone().add(x, 0, z).getBlock();
                stairBlock.setType(Material.MOSSY_COBBLESTONE_STAIRS);
                Stairs stairData = (Stairs) stairBlock.getBlockData();
                stairData.setFacing(z == 1 ? BlockFace.NORTH : BlockFace.SOUTH);
                stairBlock.setBlockData(stairData);
            }
        }
        // 십자
        stairFloor.clone().add(0, 0, 1).getBlock().setType(Material.MOSSY_COBBLESTONE_STAIRS);
        stairFloor.clone().add(0, 0, -1).getBlock().setType(Material.MOSSY_COBBLESTONE_STAIRS);
        stairFloor.clone().add(1, 0, 0).getBlock().setType(Material.MOSSY_COBBLESTONE_STAIRS);
        stairFloor.clone().add(-1, 0, 0).getBlock().setType(Material.MOSSY_COBBLESTONE_STAIRS);

        // 드래곤 알
        altarLocation.getBlock().setType(Material.DRAGON_EGG);
    }

    public void startEggBreak(Player player) {
        if (breakingTasks.containsKey(player.getUniqueId())) return;

        long duration = plugin.getGameConfigManager().getConfig().getLong("events.supply-drop.break-duration-seconds", 8);
        player.sendMessage("§d알 파괴를 시작합니다... " + duration + "초 동안 움직이지 마세요.");

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                handleEggBreak(player);
                breakingTasks.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, duration * 20L);

        breakingTasks.put(player.getUniqueId(), task);
    }

    public void cancelEggBreak(Player player) {
        if (breakingTasks.containsKey(player.getUniqueId())) {
            breakingTasks.get(player.getUniqueId()).cancel();
            breakingTasks.remove(player.getUniqueId());
            player.sendMessage("§c움직여서 알 파괴가 취소되었습니다.");
        }
    }

    private void handleEggBreak(Player player) {
        if (!isEventActive) return;

        cleanupBossBar(); // 보급 획득 시 보스바 즉시 제거

        isEventActive = false;
        Bukkit.broadcastMessage("§d[보급] §f" + player.getName() + "님이 보급을 획득했습니다!");

        // 신호기 비활성화
        altarLocation.clone().subtract(0, 2, 0).getBlock().setType(Material.GLASS);
        altarLocation.getBlock().setType(Material.AIR);

        // 보상 드랍
        generateRewards().forEach(item ->
                altarLocation.getWorld().dropItemNaturally(altarLocation, item)
        );

        // 제단 정리 예약
        long cleanupDelay = plugin.getGameConfigManager().getConfig().getLong("events.supply-drop.cleanup-delay-minutes", 1) * 20L * 60L;
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupAltar();
            }
        }.runTaskLater(plugin, cleanupDelay);
    }

    private void cleanupAltar() {
        cleanupBossBar(); // 제단 정리 시에도 보스바가 남아있지 않도록 확인
        originalBlocks.forEach((loc, data) -> loc.getBlock().setBlockData(data, false));
        originalBlocks.clear();
        altarLocation = null;
    }

    private void cleanupBossBar() {
        if (bossBarUpdateTask != null) {
            bossBarUpdateTask.cancel();
            bossBarUpdateTask = null;
        }
        if (supplyDropBossBar != null) {
            supplyDropBossBar.removeAll();
            supplyDropBossBar.setVisible(false);
            supplyDropBossBar = null;
        }
    }

    public void showBarToPlayer(Player player) {
        if (isEventActive && supplyDropBossBar != null) {
            supplyDropBossBar.addPlayer(player);
        }
    }

    private List<ItemStack> generateRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        UpgradeManager upgradeManager = plugin.getUpgradeManager();

        // 갑옷
        rewards.add(upgradeManager.setItemLevel(new ItemStack(Material.DIAMOND_HELMET), 10, Enchantment.PROTECTION, 4));
        rewards.add(upgradeManager.setItemLevel(new ItemStack(Material.DIAMOND_CHESTPLATE), 10, Enchantment.PROTECTION, 4));
        rewards.add(upgradeManager.setItemLevel(new ItemStack(Material.DIAMOND_LEGGINGS), 10, Enchantment.PROTECTION, 4));
        rewards.add(upgradeManager.setItemLevel(new ItemStack(Material.DIAMOND_BOOTS), 10, Enchantment.PROTECTION, 4));
        // 무기
        rewards.add(upgradeManager.setItemLevel(new ItemStack(Material.DIAMOND_SWORD), 10, Enchantment.SHARPNESS, 5));
        rewards.add(upgradeManager.setItemLevel(new ItemStack(Material.TRIDENT), 10, Enchantment.RIPTIDE, 3));
        rewards.add(upgradeManager.setItemLevel(new ItemStack(Material.BOW), 10, Enchantment.POWER, 5));
        rewards.add(upgradeManager.setItemLevel(new ItemStack(Material.CROSSBOW), 10, Enchantment.QUICK_CHARGE, 3));

        return rewards;
    }

    public boolean isEventActive() {
        return isEventActive;
    }

    public boolean isAltarBlock(Location location) {
        return isEventActive && altarLocation != null &&
               location.distanceSquared(altarLocation) < 25; // 제단 중심부 근처인지 대략적으로 확인
    }

    public Location getAltarLocation() {
        return altarLocation;
    }

    private void saveOriginalBlock(Location center, int radiusX, int radiusY, int radiusZ) {
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    Location loc = center.clone().add(x, y, z);
                    originalBlocks.put(loc, loc.getBlock().getBlockData());
                }
            }
        }
    }

    private Location getRandomSafeLocation(World world) {
        double borderSize = plugin.getGameConfigManager().getConfig().getDouble("world.border.overworld-size", 20000);
        double radius = (borderSize / 2.0) * 0.9;
        Random random = new Random();

        double angle = random.nextDouble() * 2 * Math.PI;
        double r = radius * Math.sqrt(random.nextDouble());
        int x = (int) (r * Math.cos(angle));
        int z = (int) (r * Math.sin(angle));

        return new Location(world, x + 0.5, 0, z + 0.5); // Y는 getHighestBlockAt으로 찾을 것이므로 0으로 둠
    }
}