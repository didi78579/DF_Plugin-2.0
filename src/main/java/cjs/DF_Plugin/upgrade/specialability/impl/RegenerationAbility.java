package cjs.DF_Plugin.upgrade.specialability.impl;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RegenerationAbility implements ISpecialAbility {
    @Override
    public String getInternalName() {
        return "regeneration";
    }

    @Override
    public String getDisplayName() {
        return "§a재생";
    }

    @Override
    public String getDescription() {
        return "§7체력이 서서히 회복됩니다.";
    }

    @Override
    public double getCooldown() {
        return DF_Main.getInstance().getGameConfigManager().getConfig().getDouble("upgrade.ability-cooldowns.regeneration", 5.0);    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event, Player player, ItemStack item) {
        // 5초(100틱) 쿨타임마다 3초간 재생 I 효과 부여
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, true));
    }
}