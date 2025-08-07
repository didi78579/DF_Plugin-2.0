package cjs.DF_Plugin.upgrade.profile;

import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface IWeaponProfile {
    void applyAttributes(ItemStack item, ItemMeta meta, int level, List<String> lore);

    default ISpecialAbility getSpecialAbility() {
        return null;
    }
}