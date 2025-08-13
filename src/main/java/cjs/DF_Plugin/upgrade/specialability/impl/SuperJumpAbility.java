package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.settings.ConfigKeys;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SuperJumpAbility implements ISpecialAbility {

    private final Map<UUID, BukkitTask> chargeTasks = new HashMap<>();
    private final Map<UUID, Boolean> isSuperJumpCharged = new HashMap<>();
    private final Map<UUID, Boolean> hasAirDashed = new HashMap<>();
    private final Map<UUID, Boolean> isSuperJumpState = new HashMap<>();

    @Override
    public String getInternalName() {
        return "super_jump";
    }

    @Override
    public String getDisplayName() { return "§b차지 점프"; }

    @Override
    public String getDescription() { return "§7웅크려 충전 후 도약, 공중에서 다시 웅크려 대쉬합니다."; }

    @Override
    public double getCooldown() {
        // 실제 쿨타임은 점프 후 개별적으로 적용됩니다.
        return DF_Main.getInstance().getGameConfigManager().getConfig().getDouble("upgrade.ability-cooldowns.super_jump", 15.0);    }

    @Override
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event, Player player, ItemStack item) {
        UUID playerUUID = player.getUniqueId();

        if (DF_Main.getInstance().getSpecialAbilityManager().isAbilityOnCooldown(player, this, item)) {
            return;
        }

        if (event.isSneaking()) {
            // 지면에서 웅크리기: 슈퍼 점프 충전 시작
            if (player.isOnGround()) {
                // 이전 작업이 있다면 취소
                cleanupChargeState(playerUUID);

                isSuperJumpCharged.put(playerUUID, false);
                isSuperJumpState.put(playerUUID, false);

                long chargeTicks = (long) (DF_Main.getInstance().getGameConfigManager().getConfig().getDouble(ConfigKeys.SUPER_JUMP_CHARGE_TIME, 3.0) * 20L);

                // 충전 완료 작업 예약
                BukkitTask chargeTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.1, 0.5, 0.05);
                    isSuperJumpCharged.put(playerUUID, true);
                    }
                }.runTaskLater(DF_Main.getInstance(), chargeTicks);
                chargeTasks.put(playerUUID, chargeTask);

            } else {
                // 공중에서 웅크리기: 공중 대쉬 실행
                if (isSuperJumpState.getOrDefault(playerUUID, false) && !hasAirDashed.getOrDefault(playerUUID, false)) {
                    performAirDash(player);
                    hasAirDashed.put(playerUUID, true);
                }
            }
        } else {
            // 웅크리기 해제
            if (chargeTasks.containsKey(playerUUID) && player.isOnGround()) {
                if (isSuperJumpCharged.getOrDefault(playerUUID, false)) {
                    performSuperJump(player, item);
                }
                cleanupChargeState(playerUUID);
            }
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event, Player player, ItemStack item) {
        UUID playerUUID = player.getUniqueId();

        // 지면에 닿았을 때 슈퍼 점프 관련 상태 초기화
        if (player.isOnGround() && isSuperJumpState.getOrDefault(playerUUID, false)) {
            isSuperJumpState.put(playerUUID, false);
            hasAirDashed.put(playerUUID, false);
        }
    }

    @Override
    public void onCleanup(Player player) {
        UUID playerUUID = player.getUniqueId();
        cleanupChargeState(playerUUID);
        isSuperJumpState.remove(playerUUID);
        hasAirDashed.remove(playerUUID);
    }

    private void cleanupChargeState(UUID playerUUID) {
        isSuperJumpCharged.remove(playerUUID);
        if (chargeTasks.containsKey(playerUUID)) {
            chargeTasks.get(playerUUID).cancel();
            chargeTasks.remove(playerUUID);
        }
    }

    private void performSuperJump(Player player, ItemStack item) {
        double jumpVelocity = DF_Main.getInstance().getGameConfigManager().getConfig().getDouble(ConfigKeys.SUPER_JUMP_VELOCITY, 2.0);
        // 1틱 늦게 속도를 적용하여 서버 물리엔진과의 충돌을 방지합니다.
        Bukkit.getScheduler().runTask(DF_Main.getInstance(), () -> {
            player.setVelocity(player.getVelocity().add(new Vector(0, jumpVelocity, 0)));
        });

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 1);
        isSuperJumpState.put(player.getUniqueId(), true);
        hasAirDashed.put(player.getUniqueId(), false);
        DF_Main.getInstance().getSpecialAbilityManager().setCooldown(player, this, item);
    }

    private void performAirDash(Player player) {
        double dashVelocityValue = DF_Main.getInstance().getGameConfigManager().getConfig().getDouble(ConfigKeys.SUPER_JUMP_DASH_VELOCITY, 2.5);
        Vector direction = player.getLocation().getDirection().normalize();
        Vector dashVelocity = direction.multiply(dashVelocityValue).setY(0.2);
        player.setVelocity(dashVelocity);
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.8f, 1.2f);
    }
}