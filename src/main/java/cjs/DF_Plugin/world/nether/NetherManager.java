package cjs.DF_Plugin.world.nether;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class NetherManager {

    private final DF_Main plugin;

    public NetherManager(DF_Main plugin) {
        this.plugin = plugin;
        startNetherHandlerTask();
    }

    private void startNetherHandlerTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getWorld().getEnvironment() != World.Environment.NETHER) {
                        continue;
                    }

                    // 1. 네더의 별 또는 화염 저항 효과가 있는지 체크
                    if (player.getInventory().contains(Material.NETHER_STAR) || player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, true, false));
                        continue;
                    }

                    // 2. 보호 수단이 없으면 화염 데미지
                    player.setFireTicks(Math.max(player.getFireTicks(), 40)); // 최소 2초간 불타도록 설정
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1초마다 실행
    }
}