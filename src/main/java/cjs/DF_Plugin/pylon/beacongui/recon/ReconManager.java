package cjs.DF_Plugin.pylon.beacongui.recon;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.pylon.config.PylonConfigManager;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ReconManager implements Listener {

    private enum ReconState {
        READY_TO_LAUNCH,
        IN_AIR,
        LANDED
    }

    private final DF_Main plugin;
    private final Map<UUID, ReconState> reconPlayers = new ConcurrentHashMap<>(); // Leader UUID -> State
    private final Map<UUID, Long> returnTimers = new ConcurrentHashMap<>(); // Leader UUID -> Teleport Timestamp
    private static final String PREFIX = PluginUtils.colorize("&c[정찰] &f");

    public ReconManager(DF_Main plugin) {
        this.plugin = plugin;
        startReturnTask();
    }

    public void activateRecon(Player player) {
        PylonConfigManager config = plugin.getPylonManager().getConfigManager();

        // 1. Check if feature is enabled
        if (!config.isReconEnabled()) {
            player.sendMessage(PREFIX + "정찰용 폭죽 기능이 비활성화되어 있습니다.");
            return;
        }

        // 2. Check if player is a clan leader
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null || !clan.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(PREFIX + "가문 대표만 정찰용 폭죽을 사용할 수 있습니다.");
            return;
        }

        // 3. Check cooldown
        long cooldownMillis = TimeUnit.HOURS.toMillis(config.getReconCooldownHours());
        long lastUsed = clan.getLastReconFireworkTime();
        if (System.currentTimeMillis() - lastUsed < cooldownMillis) {
            long remainingMillis = cooldownMillis - (System.currentTimeMillis() - lastUsed);
            String remainingTime = String.format("%02d시간 %02d분",
                    TimeUnit.MILLISECONDS.toHours(remainingMillis),
                    TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60);
            player.sendMessage(PREFIX + "다음 정찰까지 " + remainingTime + " 남았습니다.");
            return;
        }

        // 4. Check if chestplate slot is empty
        if (player.getInventory().getChestplate() != null) {
            player.sendMessage(PREFIX + "겉날개를 장착하려면 갑옷 칸을 비워야 합니다.");
            return;
        }

        // All checks passed, activate recon mode
        player.closeInventory();
        player.getInventory().setChestplate(createReconElytra());
        clan.setLastReconFireworkTime(System.currentTimeMillis());
        plugin.getClanManager().getStorageManager().saveClan(clan);
        reconPlayers.put(player.getUniqueId(), ReconState.READY_TO_LAUNCH);

        player.sendMessage(PREFIX + "정찰 모드가 활성화되었습니다. 점프하여 발사하세요!");
    }

    @EventHandler
    public void onPlayerJump(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!reconPlayers.containsKey(player.getUniqueId()) || reconPlayers.get(player.getUniqueId()) != ReconState.READY_TO_LAUNCH) {
            return;
        }

        // Detect jump by checking vertical velocity
        if (player.getVelocity().getY() > 0.1 && player.isOnGround()) { // A small threshold to detect upward movement from ground
            launchPlayer(player);
        }
    }

    private void launchPlayer(Player leader) {
        reconPlayers.put(leader.getUniqueId(), ReconState.IN_AIR);
        leader.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 444, 4, false, false));
        leader.sendMessage(PREFIX + "발사!");
    }

    @EventHandler
    public void onPlayerLand(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!reconPlayers.containsKey(player.getUniqueId()) || reconPlayers.get(player.getUniqueId()) != ReconState.IN_AIR) {
            return;
        }

        // Detect landing
        if (player.isOnGround() && event.getTo().getY() <= event.getFrom().getY()) {
            handleLanding(player);
        }
    }

    private void handleLanding(Player leader) {
        reconPlayers.put(leader.getUniqueId(), ReconState.LANDED);

        // Remove elytra
        leader.getInventory().setChestplate(null);

        // Start return timer
        int returnMinutes = plugin.getPylonManager().getConfigManager().getReconReturnMinutes();
        long returnTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(returnMinutes);
        returnTimers.put(leader.getUniqueId(), returnTime);

        leader.sendMessage(PREFIX + "착지했습니다. " + returnMinutes + "분 후에 파일런으로 귀환합니다.");
    }

    private void startReturnTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (returnTimers.isEmpty()) return;

                long now = System.currentTimeMillis();
                returnTimers.entrySet().removeIf(entry -> {
                    if (now >= entry.getValue()) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null) {
                            Clan clan = plugin.getClanManager().getClanByPlayer(entry.getKey());
                            if (clan != null && !clan.getPylonLocations().isEmpty()) {
                                String locString = clan.getPylonLocations().iterator().next();
                                Location pylonLoc = PluginUtils.deserializeLocation(locString);
                                if (pylonLoc != null) {
                                    player.teleport(pylonLoc.add(0.5, 1, 0.5));
                                    player.sendMessage(PREFIX + "파일런으로 귀환했습니다.");
                                }
                            }
                        }
                        reconPlayers.remove(entry.getKey());
                        return true; // Remove from map
                    }
                    return false;
                });
            }
        }.runTaskTimer(plugin, 0L, 20L); // Check every second
    }

    private ItemStack createReconElytra() {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = elytra.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b정찰용 겉날개");
            meta.setLore(Collections.singletonList("§7하늘을 정찰하기 위한 특수 겉날개."));
            meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            elytra.setItemMeta(meta);
        }
        return elytra;
    }
}