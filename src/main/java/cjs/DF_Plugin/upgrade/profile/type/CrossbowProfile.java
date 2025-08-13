package cjs.DF_Plugin.upgrade.profile.type;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.settings.GameConfigManager;
import cjs.DF_Plugin.upgrade.profile.IWeaponProfile;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.impl.LaserShotAbility;
import cjs.DF_Plugin.upgrade.setting.UpgradeSettingManager;
import org.bukkit.ChatColor;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CrossbowProfile implements IWeaponProfile {
    @Override
    public void applyAttributes(org.bukkit.inventory.ItemStack item, ItemMeta meta, int level, List<String> lore) {
        // 쇠뇌는 강화 레벨에 비례한 추가 피해를 로어에 직접 기록합니다.
        lore.removeIf(line -> line.contains("고정 추가 데미지:"));

        if (level > 0) {
            GameConfigManager settings = DF_Main.getInstance().getGameConfigManager();
            double damagePerLevel = settings.getConfig().getDouble("upgrade.ability-attributes.laser_shot.passive-damage-per-level", 0.5);
            double totalBonusDamage = damagePerLevel * level;
            lore.add(ChatColor.BLUE + "고정 추가 데미지: +" + String.format("%.1f", totalBonusDamage));
        }
    }

    @Override
    public ISpecialAbility getSpecialAbility() {
        return new LaserShotAbility();
    }
}