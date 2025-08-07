// C:/Users/CJS/IdeaProjects/DF_Plugin-2.0/src/main/java/cjs/DF_Plugin/upgrade/specialability/impl/VampirismAbility.java
package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.actionbar.ActionBarManager;
import cjs.DF_Plugin.upgrade.setting.UpgradeSettingManager;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class VampirismAbility implements ISpecialAbility {
    @Override
    public String getInternalName() {
        return "vampirism";
    }

    @Override
    public String getDisplayName() {
        return "§4흡혈";
    }

    @Override
    public String getDescription() {
        return "§7피격 대상의 최대 체력 40%를 흡수하고, 흡수한 체력의 50%만큼 추가 피해를 줍니다.";
    }

    @Override
    public double getCooldown() {
        return 90.0;
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event, Player player, ItemStack item) {
        SpecialAbilityManager manager = DF_Main.getInstance().getSpecialAbilityManager();
        if (manager.isAbilityOnCooldown(player, this, item)) {
            long secondsLeft = (manager.getRemainingCooldown(player, this, item) + 999) / 1000;
            ActionBarManager.sendActionBar(player, String.format("%s §e%d초", this.getDisplayName(), secondsLeft));
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        manager.setCooldown(player, this, item);

        UpgradeSettingManager settings = DF_Main.getInstance().getUpgradeSettingManager();
        double stealPercent = settings.getConfig().getDouble("ability-attributes.vampirism.health-steal-percent", 40.0) / 100.0;
        double damageMultiplier = settings.getConfig().getDouble("ability-attributes.vampirism.damage-multiplier", 0.5);

        double targetMaxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double healthToSteal = targetMaxHealth * stealPercent;
        double additionalDamage = healthToSteal * damageMultiplier;

        // 흡혈
        player.setHealth(Math.min(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + healthToSteal));
        // 추가 피해
        event.setDamage(event.getDamage() + additionalDamage);

        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.8f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.7f);
    }
}