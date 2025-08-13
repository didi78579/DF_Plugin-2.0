package cjs.DF_Plugin.upgrade.profile.type;

import cjs.DF_Plugin.upgrade.profile.IWeaponProfile;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.impl.VoidRiptideAbility;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class TridentProfile implements IWeaponProfile {
    @Override
    public void applyAttributes(ItemStack item, ItemMeta meta, int level, List<String> lore) {
        // 삼지창은 강화 레벨에 따른 기본 속성 보너스가 없으므로 비워둡니다.
        // 과거의 '추가 투사체 수' 로어는 VoidRiptideAbility에서 직접 레벨을 참조하도록 변경되어 제거되었습니다.
    }

    @Override
    public ISpecialAbility getSpecialAbility() {
        return new VoidRiptideAbility();
    }
}