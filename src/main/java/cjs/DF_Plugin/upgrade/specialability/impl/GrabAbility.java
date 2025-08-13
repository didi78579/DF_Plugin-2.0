package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.actionbar.ActionBarManager;
import cjs.DF_Plugin.settings.ConfigKeys;
import cjs.DF_Plugin.settings.GameConfigManager;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class GrabAbility implements ISpecialAbility {

    @Override
    public String getInternalName() {
        return "grab";
    }

    @Override
    public String getDisplayName() {
        return "§b끌어오기";
    }

    @Override
    public String getDescription() {
        return "§7낚시찌에 걸린 대상을 자신에게로 끌어옵니다.";
    }

    @Override
    public double getCooldown() {
        return DF_Main.getInstance().getGameConfigManager().getConfig().getDouble("upgrade.ability-cooldowns.grab", 60.0);    }

    @Override
    public void onPlayerFish(PlayerFishEvent event, Player player, ItemStack item) {
        SpecialAbilityManager manager = DF_Main.getInstance().getSpecialAbilityManager();
        GameConfigManager config = DF_Main.getInstance().getGameConfigManager();

        // 낚시찌를 던질 때 (FISHING state)
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            if (event.getHook() != null) {
                Vector velocity = event.getHook().getVelocity();

                double velocityMultiplier = config.getConfig().getDouble(ConfigKeys.GRAB_CAST_VELOCITY_MULTIPLIER, 3.0);
                double maxVelocity = config.getConfig().getDouble(ConfigKeys.GRAB_CAST_MAX_VELOCITY, 10.0);

                // 속도를 증폭시키되, 최대치를 넘지 않도록 제한합니다.
                velocity = velocity.normalize().multiply(Math.min(velocity.length() * velocityMultiplier, maxVelocity));
                event.getHook().setVelocity(velocity);
            }
            return; // 속도만 조절하고 종료
        }

        // 엔티티를 낚았을 때 (CAUGHT_ENTITY state)
        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            if (DF_Main.getInstance().getUpgradeManager().getUpgradeLevel(item) < 10) return;

            if (manager.isAbilityOnCooldown(player, this, item)) {
                long secondsLeft = (manager.getRemainingCooldown(player, this, item) + 999) / 1000;
                ActionBarManager.sendActionBar(player, String.format("%s §e%d초", this.getDisplayName(), secondsLeft));
                return;
            }

            if (!(event.getCaught() instanceof LivingEntity livingTarget)) return;

            manager.setCooldown(player, this, item);

            // 2단계 끌어오기 로직
            double upwardForceValue = config.getConfig().getDouble(ConfigKeys.GRAB_UPWARD_FORCE, 1.2);
            double pullStrength = config.getConfig().getDouble(ConfigKeys.GRAB_PULL_STRENGTH, 2.5);
            double maxPullStrength = config.getConfig().getDouble(ConfigKeys.GRAB_MAX_PULL_STRENGTH, 10.0);

            // 1. 대상 위로 띄우기
            livingTarget.setVelocity(new Vector(0, upwardForceValue, 0));

            // 2. 잠시 후 대상 끌어오기
            Location playerLocation = player.getLocation();
            DF_Main.getInstance().getServer().getScheduler().runTaskLater(DF_Main.getInstance(), () -> {
                if (!livingTarget.isValid() || livingTarget.isDead()) return;

                Location targetLocation = livingTarget.getLocation();
                Vector pullDirection = playerLocation.toVector().subtract(targetLocation.toVector()).normalize();

                Vector pullForce = pullDirection.multiply(pullStrength);
                if (pullForce.length() > maxPullStrength) {
                    pullForce = pullForce.normalize().multiply(maxPullStrength);
                }
                livingTarget.setVelocity(pullForce);
                livingTarget.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.5f, 1.0f);
            }, 10L); // 0.5초 지연
        }
    }
}