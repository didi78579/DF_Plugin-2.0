package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SuperJumpAbility implements ISpecialAbility {

    private final Map<UUID, Long> sneakStartTimes = new HashMap<>();
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
        return DF_Main.getInstance().getUpgradeSettingManager().getConfig().getDouble("ability-cooldowns.super_jump", 15.0);
    }

    @Override
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event, Player player, ItemStack item) {
        UUID playerUUID = player.getUniqueId();

        if (DF_Main.getInstance().getSpecialAbilityManager().isAbilityOnCooldown(player, this, item)) {
            return;
        }

        if (event.isSneaking()) {
            // 지면에서 웅크리기: 슈퍼 점프 충전 시작
            if (player.isOnGround()) {
                sneakStartTimes.put(playerUUID, System.currentTimeMillis());
                isSuperJumpCharged.put(playerUUID, false);
                isSuperJumpState.put(playerUUID, false);

                // 3초 후 충전 완료
                BukkitTask chargeTask = Bukkit.getScheduler().runTaskLater(DF_Main.getInstance(), () -> {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                    isSuperJumpCharged.put(playerUUID, true);
                }, 60L); // 3초
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
            if (sneakStartTimes.containsKey(playerUUID) && player.isOnGround()) {
                if (isSuperJumpCharged.getOrDefault(playerUUID, false)) {
                    performSuperJump(player, item);
                    isSuperJumpState.put(playerUUID, true);
                    hasAirDashed.put(playerUUID, false);
                }
                // 충전 상태와 관계없이 관련 데이터 정리
                isSuperJumpCharged.remove(playerUUID);
                sneakStartTimes.remove(playerUUID);
                if (chargeTasks.containsKey(playerUUID)) {
                    chargeTasks.get(playerUUID).cancel();
                    chargeTasks.remove(playerUUID);
                }
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

    private void performSuperJump(Player player, ItemStack item) {
        player.setVelocity(player.getVelocity().add(new Vector(0, 2.0, 0)));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
        DF_Main.getInstance().getSpecialAbilityManager().setCooldown(player, this, item);
    }

    private void performAirDash(Player player) {
        Vector direction = player.getLocation().getDirection().normalize();
        Vector dashVelocity = direction.multiply(2.5).setY(0.2);
        player.setVelocity(dashVelocity);
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.8f, 1.2f);
    }
}