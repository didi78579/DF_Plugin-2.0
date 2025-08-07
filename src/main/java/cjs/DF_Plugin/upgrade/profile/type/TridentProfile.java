package cjs.DF_Plugin.upgrade.profile.type;

import cjs.DF_Plugin.upgrade.profile.IWeaponProfile;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.impl.VoidRiptideAbility;
import org.bukkit.ChatColor;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class TridentProfile implements IWeaponProfile {
    @Override
    public void applyAttributes(org.bukkit.inventory.ItemStack item, ItemMeta meta, int level, List<String> lore) {
        // 과거 코드에 따라, 강화 레벨에 비례한 추가 투사체 수를 로어에 직접 기록합니다.
        lore.removeIf(line -> line.contains("추가 투사체 수:"));

        if (level > 0) {
            lore.add(ChatColor.BLUE + "추가 투사체 수: " + level + "개");
        }
    }

    @Override
    public ISpecialAbility getSpecialAbility() {
        return new VoidRiptideAbility();
    }
}