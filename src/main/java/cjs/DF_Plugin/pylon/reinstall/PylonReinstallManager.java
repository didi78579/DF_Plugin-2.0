package cjs.DF_Plugin.pylon.reinstall;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.pylon.beaconinteraction.PylonItemListener;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PylonReinstallManager {
    private final DF_Main plugin;
    private final Map<UUID, Long> reinstallDeadlines = new ConcurrentHashMap<>(); // Player UUID -> Deadline Timestamp
    private static final String PREFIX = PluginUtils.colorize("&4[파일런] &f");

    public PylonReinstallManager(DF_Main plugin) {
        this.plugin = plugin;
        startReinstallCheckTask();
    }

    public void startReinstallTimer(Player player) {
        int durationHours = plugin.getPylonManager().getConfigManager().getReinstallDurationHours();
        long deadline = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(durationHours);
        reinstallDeadlines.put(player.getUniqueId(), deadline);
        player.sendMessage(PREFIX + "§c파일런을 " + durationHours + "시간 내에 다시 설치해야 합니다!");
    }

    public void cancelReinstallTimer(Player player) {
        if (reinstallDeadlines.remove(player.getUniqueId()) != null) {
            player.sendMessage(PREFIX + "§a파일런 재설치 타이머가 취소되었습니다.");
        }
    }

    private void removePylonItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (PylonItemListener.isPylonItem(item)) {
                player.getInventory().remove(item);
                break; // Assume only one pylon item
            }
        }
    }

    private void startReinstallCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (reinstallDeadlines.isEmpty()) return;

                long now = System.currentTimeMillis();
                reinstallDeadlines.forEach((uuid, deadline) -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) return;

                    long remainingMillis = deadline - now;
                    if (remainingMillis <= 0) {
                        player.sendMessage(PREFIX + "§4재설치 시간이 초과되어 파일런이 소멸했습니다.");
                        removePylonItem(player);
                        reinstallDeadlines.remove(uuid);
                        return;
                    }

                    long remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis);

                    if (remainingSeconds <= 60 && remainingSeconds > 0 && remainingSeconds % 10 == 0) {
                        player.sendMessage(PREFIX + "§c재설치까지 남은 시간: " + remainingSeconds + "초");
                    } else if (remainingSeconds <= 600 && remainingSeconds % 60 == 0) {
                        player.sendMessage(PREFIX + "§c재설치까지 남은 시간: " + TimeUnit.SECONDS.toMinutes(remainingSeconds) + "분");
                    }
                });
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L); // Run every second
    }
}