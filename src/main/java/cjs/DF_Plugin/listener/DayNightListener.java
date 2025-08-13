package cjs.DF_Plugin.listener;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class DayNightListener implements Listener {

    private final DF_Main plugin;
    private final Random random = new Random();
    private long lastCheckedTime = -1;

    public DayNightListener(DF_Main plugin) {
        this.plugin = plugin;
        startDayCheckTask();
    }

    private void startDayCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getGameStartManager().isGameStarted()) return;

                World world = plugin.getServer().getWorlds().get(0);
                long currentTime = world.getTime();

                // 시간이 0에 가까워질 때 (새로운 날이 시작될 때)
                if (lastCheckedTime > 1000 && currentTime < 1000) {
                    double chance = plugin.getGameConfigManager().getConfig().getDouble("events.supply-drop.chance-per-day", 0.05);
                    if (random.nextDouble() < chance) {
                        plugin.getSupplyDropManager().triggerEvent();
                    }
                }
                lastCheckedTime = currentTime;
            }
        }.runTaskTimer(plugin, 0L, 20L * 10); // 10초마다 확인
    }
}