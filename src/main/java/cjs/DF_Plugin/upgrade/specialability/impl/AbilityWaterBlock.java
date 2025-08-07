package cjs.DF_Plugin.upgrade.specialability.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class AbilityWaterBlock {

    private final Player player;
    private final JavaPlugin plugin;

    public AbilityWaterBlock(Player player, JavaPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    public void startWater() {
        Location loc = player.getLocation();
        Block block = loc.getBlock();

        if (block.getType() == Material.AIR) {
            block.setType(Material.WATER);
            new BukkitRunnable() {
                public void run() { block.setType(Material.AIR); }
            }.runTaskLater(plugin, 20L); // 1초 후 물 제거
        }
    }
}