// C:/Users/CJS/IdeaProjects/DF_Plugin-2.0/src/main/java/cjs/DF_Plugin/upgrade/specialability/impl/StunAbility.java
package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.actionbar.ActionBarManager;
import cjs.DF_Plugin.settings.GameConfigManager;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class StunAbility implements ISpecialAbility {
    @Override
    public String getInternalName() {
        return "stun";
    }

    @Override
    public String getDisplayName() {
        return "§8기절";
    }

    @Override
    public String getDescription() {
        return "§7피격 대상을 1.5초간 행동 불가 상태로 만듭니다.";
    }

    @Override
    public double getCooldown() {
        return 120.0;
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

        GameConfigManager configManager = DF_Main.getInstance().getGameConfigManager();
        int durationTicks = (int) (configManager.getConfig().getDouble("upgrade.ability-attributes.stun.duration-seconds", 1.5) * 20);

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 5, true, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, durationTicks, 5, true, false));
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);

        if (target instanceof Player targetPlayer) {
            final Location initialLocation = targetPlayer.getLocation();
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks++ >= durationTicks || !targetPlayer.isOnline()) {
                        this.cancel();
                        return;
                    }
                    targetPlayer.teleport(initialLocation);
                }
            }.runTaskTimer(DF_Main.getInstance(), 0L, 1L);
        }
    }
}