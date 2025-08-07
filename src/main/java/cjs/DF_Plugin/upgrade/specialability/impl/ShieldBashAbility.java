package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class ShieldBashAbility implements ISpecialAbility {
    @Override
    public String getInternalName() {
        return "shield_bash";
    }

    @Override
    public String getDisplayName() {
        return "§e방패 돌격";
    }

    @Override
    public String getDescription() {
        return "§7전방으로 돌격하며, 경로상의 적을 띄웁니다.";
    }

    @Override
    public double getCooldown() {
        return 150.0;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event, Player player, ItemStack item) {
        SpecialAbilityManager manager = DF_Main.getInstance().getSpecialAbilityManager();
        if (manager.isAbilityOnCooldown(player, this, item)) {
            return;
        }
        manager.setCooldown(player, this, item);

        // 대쉬 로직
        Vector dashDirection = player.getLocation().getDirection().multiply(5); // 5 블록 거리
        player.setVelocity(dashDirection);

        // 효과: 연기 파티클, 사운드
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.5f);

        // 대쉬 후 효과
        new BukkitRunnable() {
            @Override
            public void run() {
                Location playerLocation = player.getLocation();
                List<Entity> nearbyEntities = player.getNearbyEntities(2, 1, 2);

                // 주변 적 공중 띄우기
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        livingEntity.setVelocity(new Vector(0, 1, 0)); // 위로 띄우기
                    }
                }
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 1);
            }
        }.runTaskLater(DF_Main.getInstance(), 6L);
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event, Player player, ItemStack item) {
        // 방패를 들고 막고 있을 때, 도끼에 의해 방패가 무력화되는 것을 방지
        if (player.isBlocking() && event.getDamager() instanceof Player) {
            ItemStack damagerWeapon = ((Player) event.getDamager()).getInventory().getItemInMainHand();
            if (damagerWeapon.getType().name().endsWith("_AXE")) {
                event.setCancelled(true);
            }
        }
    }
}