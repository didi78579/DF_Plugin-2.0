package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class DamageNegationAbility implements ISpecialAbility {
    @Override
    public String getInternalName() {
        return "damage_negation";
    }

    @Override
    public String getDisplayName() {
        return "§c피해 무효화";
    }

    @Override
    public String getDescription() {
        return "§7공격받았을 때, 일정 확률로 피해를 무효화합니다.";
    }

    @Override
    public double getCooldown() {
        return DF_Main.getInstance().getGameConfigManager().getConfig().getDouble("upgrade.ability-cooldowns.damage_negation", 0.0); // 확률 기반이므로 쿨타임 없음
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event, Player player, ItemStack item) {
        double chance = DF_Main.getInstance().getGameConfigManager().getConfig().getDouble("upgrade.ability-chances.damage_negation", 0.1);
        if (Math.random() < chance) {
            event.setCancelled(true);
            // 금속이 무언가를 튕겨내는 듯한 날카로운 소리로 변경
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.2f);
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        }
    }
}