// C:/Users/CJS/IdeaProjects/DF_Plugin-2.0/src/main/java/cjs/DF_Plugin/upgrade/specialability/impl/ParricideAbility.java
package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.actionbar.ActionBarManager;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class ParricideAbility implements ISpecialAbility {
    @Override
    public String getInternalName() {
        return "parricide";
    }

    @Override
    public String getDisplayName() {
        return "§c패륜";
    }

    @Override
    public String getDescription() {
        return "§7피격 대상에게 번개를 내리치고, 모든 효과를 제거하며 방패를 30초간 무력화합니다.";
    }

    @Override
    public double getCooldown() {
        return 60.0;
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event, Player player, ItemStack item) {
        SpecialAbilityManager manager = DF_Main.getInstance().getSpecialAbilityManager();
        if (manager.isAbilityOnCooldown(player, this, item)) {
            ActionBarManager.sendActionBar(player, String.format("§c%s 재사용 대기시간: %.1f초", this.getDisplayName(), manager.getRemainingCooldown(player, this, item) / 1000.0));
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        manager.setCooldown(player, this, item);

        target.getWorld().strikeLightning(target.getLocation());
        target.getActivePotionEffects().forEach(effect -> target.removePotionEffect(effect.getType()));

        if (target instanceof Player targetPlayer) {
            targetPlayer.setCooldown(Material.SHIELD, 20 * 30); // 30초
        }
    }
}