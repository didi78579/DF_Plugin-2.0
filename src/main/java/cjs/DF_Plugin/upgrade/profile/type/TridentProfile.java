package cjs.DF_Plugin.upgrade.profile.type;

import cjs.DF_Plugin.upgrade.profile.IUpgradeableProfile;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.impl.BackflowAbility;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class TridentProfile implements IUpgradeableProfile {
    @Override
    public void applyAttributes(ItemStack item, ItemMeta meta, int level, List<String> lore) {
        if (level > 0) {
            lore.add(""); // 간격
            lore.add("§7추가 투사체: §b+" + level + "개");
        }
    }

    @Override
    public ISpecialAbility getSpecialAbility() {
        return new BackflowAbility();
    }
}