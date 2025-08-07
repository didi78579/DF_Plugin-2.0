package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.actionbar.ActionBarManager;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import org.bukkit.entity.Entity;
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
        return DF_Main.getInstance().getUpgradeSettingManager().getConfig().getDouble("ability-cooldowns.grab", 60.0);
    }

    @Override
    public void onPlayerFish(PlayerFishEvent event, Player player, ItemStack item) {
        SpecialAbilityManager manager = DF_Main.getInstance().getSpecialAbilityManager();

        // 낚시찌를 던질 때 (FISHING state)
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            if (!manager.isAbilityOnCooldown(player, this, item)) {
                // 쿨타임이 아닐 때 낚시찌 속도 5배 증가
                event.getHook().setVelocity(event.getHook().getVelocity().multiply(5.0));
            }
            return; // 속도만 조절하고 종료
        }

        // 엔티티를 낚았을 때 (CAUGHT_ENTITY state)
        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            // 10강 전용 능력입니다.
            if (DF_Main.getInstance().getUpgradeManager().getUpgradeLevel(item) < 10) return;

            if (manager.isAbilityOnCooldown(player, this, item)) {
                long secondsLeft = (manager.getRemainingCooldown(player, this, item) + 999) / 1000;
                ActionBarManager.sendActionBar(player, String.format("%s §e%d초", this.getDisplayName(), secondsLeft));
                return;
            }

            Entity caught = event.getCaught();
            if (caught != null) {
                double pullStrength = DF_Main.getInstance().getUpgradeSettingManager().getConfig().getDouble("ability-attributes.grab.pull-strength", 2.5);
                Vector direction = player.getLocation().toVector().subtract(caught.getLocation().toVector()).normalize();
                caught.setVelocity(direction.multiply(pullStrength));
                manager.setCooldown(player, this, item); // 여기서 쿨다운 설정
            }
        }
    }
}