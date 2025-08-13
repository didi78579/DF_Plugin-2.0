package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.actionbar.ActionBarManager;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import org.bukkit.GameMode;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;

public class DoubleJumpAbility implements ISpecialAbility {

    @Override
    public String getInternalName() { return "double_jump"; }

    @Override
    public String getDisplayName() { return "§e더블 점프"; }

    @Override
    public String getDescription() { return "§7공중에서 도약하고, 낙하 피해를 받지 않습니다. PvP 시 쿨타임이 적용됩니다."; }

    @Override
    public double getCooldown() {
        // 기본 쿨타임은 없으며, PvP 피격 시에만 별도로 적용됩니다.
        return 0.0;
    }

    @Override
    public void onToggleFlight(PlayerToggleFlightEvent event, Player player, ItemStack item) {
        // 크리에이티브 또는 관전 모드에서는 기본 비행을 허용합니다.
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // 서바이벌/어드벤처 모드에서는 기본 비행을 항상 취소하고, 비행 상태를 강제로 해제합니다.
        // 이것이 쿨다운 중에도 비행이 활성화되는 버그를 막는 핵심입니다.
        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);

        // 이제 쿨다운을 확인합니다.
        SpecialAbilityManager manager = DF_Main.getInstance().getSpecialAbilityManager();
        if (manager.isAbilityOnCooldown(player, this, item)) {
            long secondsLeft = (manager.getRemainingCooldown(player, this, item) + 999) / 1000;
            ActionBarManager.sendActionBar(player, String.format("%s §e%d초", this.getDisplayName(), secondsLeft));
            return; // 비행은 이미 취소되었으므로, 메시지만 보내고 종료합니다.
        }

        // 쿨다운이 아니라면, 더블 점프를 실행합니다.
        double dashVelocityMultiplier = DF_Main.getInstance().getGameConfigManager().getConfig().getDouble("upgrade.ability-attributes.double_jump.dash-velocity-multiplier", 1.3);
        double dashYVelocity = DF_Main.getInstance().getGameConfigManager().getConfig().getDouble("upgrade.ability-attributes.double_jump.dash-y-velocity", 0.5);

        player.setVelocity(player.getLocation().getDirection().multiply(dashVelocityMultiplier).setY(dashYVelocity));
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f);
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event, Player player, ItemStack item) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event, Player player, ItemStack item) {
        // 'player'는 이 능력을 가진 피해자(victim)입니다.
        // 공격자가 다른 플레이어일 경우에만 쿨다운을 적용합니다.
        if (event.getDamager() instanceof Player) {
            SpecialAbilityManager manager = DF_Main.getInstance().getSpecialAbilityManager();
            double pvpCooldown = DF_Main.getInstance().getGameConfigManager().getConfig().getDouble("upgrade.ability-attributes.double_jump.pvp-cooldown-seconds", 30.0);
            manager.setCooldown(player, this, item, pvpCooldown);
            ActionBarManager.sendActionBar(player, String.format("§c피격으로 인해 §b%s§c 재사용 대기시간이 적용됩니다: §e%d초", ChatColor.stripColor(this.getDisplayName()), (long) pvpCooldown));
        }
    }
}