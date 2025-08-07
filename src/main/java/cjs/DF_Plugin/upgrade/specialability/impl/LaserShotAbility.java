package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.setting.UpgradeSettingManager;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class LaserShotAbility implements ISpecialAbility {
    @Override
    public String getInternalName() {
        return "laser_shot";
    }

    @Override
    public String getDisplayName() {
        return "§c레이저 발사";
    }

    @Override
    public String getDescription() {
        return "§7쇠뇌에서 레이저를 발사합니다.";
    }

    @Override
    public double getCooldown() {
        return 0; // This ability is passive or level-gated, no active cooldown.
    }

    @Override
    public void onEntityShootBow(EntityShootBowEvent event, Player player, ItemStack item) {
        int level = DF_Main.getInstance().getUpgradeManager().getUpgradeLevel(item);
        UpgradeSettingManager settings = DF_Main.getInstance().getUpgradeSettingManager();
        int requiredLevel = settings.getConfig().getInt("ability-attributes.laser_shot.required-level", 10);

        if (level >= requiredLevel) {
            if (event.getProjectile() instanceof Arrow arrow) {
                double velocityMultiplier = settings.getConfig().getDouble("ability-attributes.laser_shot.velocity-multiplier", 3.0);
                arrow.setVelocity(arrow.getVelocity().multiply(velocityMultiplier));
            }
        }
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event, Player player, ItemStack item) {
        if (!(event.getDamager() instanceof Arrow) || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        int level = DF_Main.getInstance().getUpgradeManager().getUpgradeLevel(item);
        if (level <= 0) return;

        UpgradeSettingManager settings = DF_Main.getInstance().getUpgradeSettingManager();

        // 1. Passive damage bonus for all levels
        double damagePerLevel = settings.getConfig().getDouble("ability-attributes.laser_shot.passive-damage-per-level", 0.5);
        double additionalDamage = level * damagePerLevel;
        event.setDamage(event.getDamage() + additionalDamage);

        // 2. Special effect at required level
        int requiredLevel = settings.getConfig().getInt("ability-attributes.laser_shot.required-level", 10);
        if (level >= requiredLevel) {
            int glowDuration = (int) (settings.getConfig().getDouble("ability-attributes.laser_shot.glow-duration-seconds", 10) * 20);
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowDuration, 0));
        }
    }
}