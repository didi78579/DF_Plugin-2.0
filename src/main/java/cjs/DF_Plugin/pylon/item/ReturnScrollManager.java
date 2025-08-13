package cjs.DF_Plugin.pylon.item;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ReturnScrollManager {

    private final DF_Main plugin;
    private final Map<UUID, BukkitTask> castingPlayers = new HashMap<>();

    public ReturnScrollManager(DF_Main plugin) {
        this.plugin = plugin;
    }

    public void startCasting(Player player) {
        if (castingPlayers.containsKey(player.getUniqueId())) {
            player.sendMessage("§c이미 귀환을 시도하고 있습니다.");
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null || clan.getPylonLocations().isEmpty()) {
            player.sendMessage("§c귀환할 파일런이 없습니다.");
            return;
        }

        World.Environment environment = player.getWorld().getEnvironment();
        if (environment == World.Environment.NETHER && !plugin.getGameConfigManager().isReturnScrollAllowedInNether()) {
            player.sendMessage("§c이곳에서는 귀환 주문서를 사용할 수 없습니다.");
            return;
        }
        if (environment == World.Environment.THE_END && !plugin.getGameConfigManager().isReturnScrollAllowedInEnd()) {
            player.sendMessage("§c이곳에서는 귀환 주문서를 사용할 수 없습니다.");
            return;
        }

        int castTime = plugin.getGameConfigManager().getReturnScrollCastTime();
        player.sendMessage("§b" + castTime + "초 후 파일런으로 귀환합니다... (피격 시 취소)");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.5f);

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int totalTicks = castTime * 20;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelCasting(player, false);
                    return;
                }

                if (ticks >= totalTicks) {
                    teleportToPylonArea(player, clan);
                    player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                    player.sendMessage("§b파일런 영역으로 귀환했습니다.");
                    castingPlayers.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                spawnMagicCircle(player.getLocation());
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);

        castingPlayers.put(player.getUniqueId(), task);
    }

    public void cancelCasting(Player player, boolean showMessage) {
        if (castingPlayers.containsKey(player.getUniqueId())) {
            castingPlayers.get(player.getUniqueId()).cancel();
            castingPlayers.remove(player.getUniqueId());
            if (showMessage) {
                player.sendMessage("§c귀환이 취소되었습니다.");
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.0f);
            }
        }
    }

    public boolean isCasting(Player player) {
        return castingPlayers.containsKey(player.getUniqueId());
    }

    private void teleportToPylonArea(Player player, Clan clan) {
        List<String> pylonLocations = new ArrayList<>(clan.getPylonLocations());
        Collections.shuffle(pylonLocations);
        Location pylonCenter = PluginUtils.deserializeLocation(pylonLocations.get(0));

        if (pylonCenter == null) {
            player.sendMessage("§c귀환 위치를 찾는데 실패했습니다. 스폰 지역으로 이동합니다.");
            player.teleport(player.getWorld().getSpawnLocation());
            return;
        }

        Random random = new Random();
        int radius = plugin.getGameConfigManager().getPylonAreaEffectRadius();
        Location safeLoc = pylonCenter.getWorld().getHighestBlockAt(pylonCenter).getLocation().add(0.5, 1, 0.5);
        player.teleport(safeLoc);
    }

    private void spawnMagicCircle(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        double radius = 1.5;
        for (int i = 0; i < 360; i += 15) {
            double angle = Math.toRadians(i);
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            world.spawnParticle(Particle.ENCHANT, center.clone().add(x, 0.2, z), 1, 0, 0, 0, 0);
        }
    }
}