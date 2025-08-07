package cjs.DF_Plugin.upgrade.profile.type;

import cjs.DF_Plugin.upgrade.profile.IWeaponProfile;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.impl.ShieldBashAbility;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ShieldProfile implements IWeaponProfile {
    @Override
    public void applyAttributes(org.bukkit.inventory.ItemStack item, ItemMeta meta, int level, List<String> lore) {
        // 강화 레벨만큼 내구성 인챈트 적용
        if (level > 0) {
            meta.addEnchant(Enchantment.UNBREAKING, level, true);
        } else {
            meta.removeEnchant(Enchantment.UNBREAKING);
        }
    }

    @Override
    public ISpecialAbility getSpecialAbility() {
        return new ShieldBashAbility();
    }
}