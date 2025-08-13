package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.settings.ConfigKeys;
import cjs.DF_Plugin.settings.GameConfigManager;
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

    private final Map<UUID, BukkitTask> chargeTasks = new HashMap<>(); // 충전 시작 -> 완료까지의 작업
    private final Map<UUID, BukkitTask> chargedStateTasks = new HashMap<>(); // 충전 완료 후 발사 대기 상태의 작업
    private static final String SUPERCHARGED_ARROW_KEY = "supercharged_arrow"; // 발사된 화살 메타데이터
    private static final String CHARGED_STATE_KEY = "supercharge_charged_state"; // 플레이어 충전 완료 상태 메타데이터
    private static final String PASSIVE_LEVEL_KEY = "supercharge_passive_level"; // 화살에 강화 레벨 저장

    @Override
    public String getInternalName() {
        return "supercharge";
    }

    @Override
    public String getDisplayName() {
        return "§6슈퍼차지";
    }

    @Override
    public String getDescription() {
        return "§7활을 당겨 충전 후 강력한 화살을 발사, 패시브로 추가 피해를 줍니다.";
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

        GameConfigManager configManager = DF_Main.getInstance().getGameConfigManager();
        long chargeTicks = (long) (configManager.getConfig().getDouble(ConfigKeys.SUPERCHARGE_CHARGE_TIME, 8.0) * 20L);

        BukkitTask chargeTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 활을 계속 당기고 있는지 확인
                if (player.isOnline() && player.isHandRaised() && item.equals(player.getInventory().getItemInMainHand())) {
                    enterChargedState(player, item);
                }
                // 작업이 완료되거나 조건이 깨지면 맵에서 제거
                chargeTasks.remove(player.getUniqueId());
            }
        }.runTaskLater(DF_Main.getInstance(), chargeTicks);

        chargeTasks.put(player.getUniqueId(), chargeTask);
    }

    /**
     * 슈퍼차지 충전이 완료된 후, 발사 대기 상태로 전환하고 관련 효과를 재생합니다.
     * @param player 대상 플레이어
     * @param bow 사용 중인 활
     */
    private void enterChargedState(Player player, ItemStack bow) {
        // 이미 다른 대기 상태 작업이 있다면 취소
        if (chargedStateTasks.containsKey(player.getUniqueId())) {
            chargedStateTasks.get(player.getUniqueId()).cancel();
        }

        player.setMetadata(CHARGED_STATE_KEY, new FixedMetadataValue(DF_Main.getInstance(), true));
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);

        GameConfigManager configManager = DF_Main.getInstance().getGameConfigManager();
        long durationTicks = (long) (configManager.getConfig().getDouble(ConfigKeys.SUPERCHARGE_CHARGED_DURATION, 5.0) * 20L);

        BukkitTask chargedTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 플레이어가 접속 중이고, 여전히 해당 활을 들고 있는지 확인
                if (!player.isOnline() || !bow.equals(player.getInventory().getItemInMainHand())) {
                    cleanupChargedState(player);
                    return;
                }
                // 지속 시간 동안 파티클 효과 재생
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3, 0.01);
            }
        }.runTaskTimer(DF_Main.getInstance(), 0L, 2L);

        // 지속 시간이 지나면 자동으로 상태 해제
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupChargedState(player);
            }
        }.runTaskLater(DF_Main.getInstance(), durationTicks);

        chargedStateTasks.put(player.getUniqueId(), chargedTask);
    }

    @Override
    public void onEntityShootBow(EntityShootBowEvent event, Player player, ItemStack item) {
        Arrow arrow = (Arrow) event.getProjectile();
        GameConfigManager configManager = DF_Main.getInstance().getGameConfigManager();

        // 슈퍼차지 상태 확인 및 메타데이터 이전
        if (player.hasMetadata(CHARGED_STATE_KEY)) {
            cleanupChargedState(player); // 발사했으므로 대기 상태 정리
            arrow.setMetadata(SUPERCHARGED_ARROW_KEY, new FixedMetadataValue(DF_Main.getInstance(), true));
            arrow.setKnockbackStrength(arrow.getKnockbackStrength() + configManager.getConfig().getInt(ConfigKeys.SUPERCHARGE_KNOCKBACK_BONUS, 2));
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
            applyFlameTrail(arrow);
        }

        // 패시브 효과를 위해 화살에 강화 레벨 저장
        UpgradeManager upgradeManager = DF_Main.getInstance().getUpgradeManager();
        int level = upgradeManager.getUpgradeLevel(item);
        if (level > 0) {
            arrow.setMetadata(PASSIVE_LEVEL_KEY, new FixedMetadataValue(DF_Main.getInstance(), level));
        }

        // 발사 시, 진행 중이던 충전 작업이 있었다면 취소
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

        GameConfigManager configManager = DF_Main.getInstance().getGameConfigManager();

        // 슈퍼차지 화살 데미지
        if (arrow.hasMetadata(SUPERCHARGED_ARROW_KEY)) {
            event.setDamage(event.getDamage() + configManager.getConfig().getDouble(ConfigKeys.SUPERCHARGE_DAMAGE, 20.0));
            target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1);
        } else if (arrow.hasMetadata(PASSIVE_LEVEL_KEY)) { // 슈퍼차지가 아닐 경우에만 패시브 데미지 적용
            // 일반 강화 화살 데미지
            int level = arrow.getMetadata(PASSIVE_LEVEL_KEY).get(0).asInt();
            if (level > 0) {
                double percentPerLevel = configManager.getConfig().getDouble(ConfigKeys.SUPERCHARGE_PASSIVE_DAMAGE_PERCENT, 1.5) / 100.0;
                double maxPercent = configManager.getConfig().getDouble(ConfigKeys.SUPERCHARGE_PASSIVE_DAMAGE_MAX_PERCENT, 15.0) / 100.0;
                double healthPercentage = Math.min(percentPerLevel * level, maxPercent);
                double additionalDamage = target.getHealth() * healthPercentage;
                event.setDamage(event.getDamage() + additionalDamage);
            }
        }
    }

    /**
     * 플레이어의 슈퍼차지 대기 상태와 관련된 모든 효과와 데이터를 정리합니다.
     * @param player 대상 플레이어
     */
    private void cleanupChargedState(Player player) {
        if (player.hasMetadata(CHARGED_STATE_KEY)) {
            player.removeMetadata(CHARGED_STATE_KEY, DF_Main.getInstance());
        }
        if (chargedStateTasks.containsKey(player.getUniqueId())) {
            chargedStateTasks.get(player.getUniqueId()).cancel();
            chargedStateTasks.remove(player.getUniqueId());
        }
    }

    /**
     * 발사된 화살에 화염 파티클 흔적을 추가합니다.
     * @param arrow 파티클을 추가할 화살
     */
    private void applyFlameTrail(Arrow arrow) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || arrow.isOnGround()) {
                    this.cancel();
                    return;
                }
                arrow.getWorld().spawnParticle(Particle.FLAME, arrow.getLocation(), 2, 0, 0, 0, 0.01);
            }
        }.runTaskTimer(DF_Main.getInstance(), 0L, 1L);
    }
}