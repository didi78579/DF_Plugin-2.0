package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.UpgradeManager;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SuperchargeBowAbility implements ISpecialAbility {

    private final Map<UUID, BukkitTask> chargeTasks = new HashMap<>();
    private final String SUPERCHARGED_ARROW_KEY = "supercharged_arrow";

    @Override
    public String getInternalName() {
        return "supercharge_bow";
    }

    @Override
    public String getDisplayName() {
        return "§6슈퍼차지";
    }

    @Override
    public String getDescription() {
        return "§7활을 당겨 충전 후 강력한 화살을 발사합니다.";
    }

    @Override
    public double getCooldown() {
        return 0; // 자체적으로 관리
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event, Player player, ItemStack item) {
        // 이미 차징 중이면 중복 실행 방지
        if (chargeTasks.containsKey(player.getUniqueId())) {
            return;
        }

        // 8초 후 슈퍼차지 활성화
        BukkitTask chargeTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 활을 계속 당기고 있는지 확인
                if (player.isHandRaised() && player.getInventory().getItemInMainHand().equals(item)) {
                    player.setMetadata(SUPERCHARGED_ARROW_KEY, new FixedMetadataValue(DF_Main.getInstance(), true));
                    player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                    player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);
                }
                chargeTasks.remove(player.getUniqueId());
            }
        }.runTaskLater(DF_Main.getInstance(), 160L); // 8초

        chargeTasks.put(player.getUniqueId(), chargeTask);
    }

    @Override
    public void onEntityShootBow(EntityShootBowEvent event, Player player, ItemStack item) {
        // 슈퍼차지 상태 확인 및 메타데이터 이전
        if (player.hasMetadata(SUPERCHARGED_ARROW_KEY)) {
            Arrow arrow = (Arrow) event.getProjectile();
            arrow.setMetadata(SUPERCHARGED_ARROW_KEY, new FixedMetadataValue(DF_Main.getInstance(), true));
            arrow.setCritical(true);
            arrow.setKnockbackStrength(arrow.getKnockbackStrength() + 2);
            player.removeMetadata(SUPERCHARGED_ARROW_KEY, DF_Main.getInstance());
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
        }

        // 차징 작업이 있었다면 취소
        if (chargeTasks.containsKey(player.getUniqueId())) {
            chargeTasks.get(player.getUniqueId()).cancel();
            chargeTasks.remove(player.getUniqueId());
        }
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event, Player player, ItemStack item) {
        if (!(event.getDamager() instanceof Arrow arrow) || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        // 슈퍼차지 화살 데미지
        if (arrow.hasMetadata(SUPERCHARGED_ARROW_KEY)) {
            event.setDamage(event.getDamage() + 20.0); // 10칸 고정 추가 데미지
            target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1);
        } else {
            // 일반 강화 화살 데미지
            int level = DF_Main.getInstance().getUpgradeManager().getUpgradeLevel(item);
            if (level > 0) {
                double healthPercentage = Math.min(0.015 * level, 0.15);
                double additionalDamage = target.getHealth() * healthPercentage;
                event.setDamage(event.getDamage() + additionalDamage);
            }
        }
    }
}