package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.settings.GameConfigManager;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.settings.ConfigKeys;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.metadata.FixedMetadataValue;

public class LaserShotAbility implements ISpecialAbility {

    private static final String LASER_SHOT_LEVEL_KEY = "laser_shot_level";

    @Override
    public String getInternalName() {
        return "laser_shot";
    }

    @Override
    public String getDisplayName() {
        return "§c레이저 샷";
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
        if (!(event.getProjectile() instanceof Arrow arrow)) {
            return;
        }

        int level = DF_Main.getInstance().getUpgradeManager().getUpgradeLevel(item);
        GameConfigManager settings = DF_Main.getInstance().getGameConfigManager();
        int requiredLevel = settings.getConfig().getInt(ConfigKeys.LASER_SHOT_REQUIRED_LEVEL, 10);

        // 화살에 강화 레벨을 저장하여, 명중 시 정확한 피해를 계산하도록 합니다.
        if (level > 0) {
            arrow.setMetadata(LASER_SHOT_LEVEL_KEY, new FixedMetadataValue(DF_Main.getInstance(), level));
        }

        if (level >= requiredLevel) {
            double velocityMultiplier = settings.getConfig().getDouble(ConfigKeys.LASER_SHOT_VELOCITY_MULTIPLIER, 3.0);
            arrow.setVelocity(arrow.getVelocity().multiply(velocityMultiplier));
        }
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event, Player player, ItemStack item) {
        if (!(event.getDamager() instanceof Arrow arrow) || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        // 발사 시 화살에 저장된 레벨 정보를 가져옵니다.
        if (!arrow.hasMetadata(LASER_SHOT_LEVEL_KEY)) {
            return;
        }

        int level = arrow.getMetadata(LASER_SHOT_LEVEL_KEY).get(0).asInt();
        if (level <= 0) return;

        GameConfigManager settings = DF_Main.getInstance().getGameConfigManager();

        // 1. 레벨 비례 추가 데미지 계산
        double damagePerLevel = settings.getConfig().getDouble(ConfigKeys.LASER_SHOT_PASSIVE_DAMAGE, 0.5);
        double passiveBonusDamage = level * damagePerLevel;

        // 2. 속도 증가로 인한 기본 데미지 증폭을 보정하고, 패시브 데미지를 더합니다.
        int requiredLevel = settings.getConfig().getInt(ConfigKeys.LASER_SHOT_REQUIRED_LEVEL, 10);
        double finalDamage;

        if (level >= requiredLevel) {
            // 속도가 증폭된 경우, 기본 데미지를 원래대로 되돌립니다.
            double velocityMultiplier = settings.getConfig().getDouble(ConfigKeys.LASER_SHOT_VELOCITY_MULTIPLIER, 3.0);
            double originalBaseDamage = event.getDamage() / velocityMultiplier;
            finalDamage = originalBaseDamage + passiveBonusDamage;
        } else {
            // 속도 증폭이 없었으므로, 기존 데미지에 패시브 보너스만 더합니다.
            finalDamage = event.getDamage() + passiveBonusDamage;
        }
        event.setDamage(finalDamage);

        // 3. Special effect at required level
        if (level >= requiredLevel) {
            int glowDuration = (int) (settings.getConfig().getDouble(ConfigKeys.LASER_SHOT_GLOW_DURATION, 10) * 20);
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowDuration, 0));
        }
    }
}