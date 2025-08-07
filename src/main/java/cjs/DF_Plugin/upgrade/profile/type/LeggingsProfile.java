package cjs.DF_Plugin.upgrade.profile.type;

import cjs.DF_Plugin.upgrade.profile.IWeaponProfile;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.impl.SuperJumpAbility;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class LeggingsProfile implements IWeaponProfile {
    @Override
    public void applyAttributes(org.bukkit.inventory.ItemStack item, ItemMeta meta, int level, List<String> lore) {
        final String ATTRIBUTE_NAME = "upgrade.health";

        // 1. 아이템의 모든 관련 속성을 초기화합니다.
        meta.removeAttributeModifier(Attribute.GENERIC_ARMOR);
        meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
        meta.removeAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        meta.removeAttributeModifier(Attribute.GENERIC_MAX_HEALTH);

        // 2. 아이템의 기본 방어 관련 속성들을 다시 적용합니다.
        applyBaseArmorAttributes(item.getType(), meta);

        // 3. 새로운 강화 속성(체력)을 계산하고 적용합니다.
        // 강화 레벨당 체력 2 증가 (과거 코드 로직)
        double totalValue = level * 2.0;
        if (totalValue > 0) {
            AttributeModifier mod = new AttributeModifier(UUID.randomUUID(), ATTRIBUTE_NAME, totalValue, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS);
            meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, mod);
        }
    }

    private void applyBaseArmorAttributes(Material material, ItemMeta meta) {
        double armor = 0, toughness = 0, knockbackResistance = 0;

        switch (material) {
            case LEATHER_LEGGINGS -> armor = 2;
            case GOLDEN_LEGGINGS -> armor = 3;
            case CHAINMAIL_LEGGINGS -> armor = 4;
            case IRON_LEGGINGS -> armor = 5;
            case DIAMOND_LEGGINGS -> { armor = 6; toughness = 2; }
            case NETHERITE_LEGGINGS -> { armor = 6; toughness = 3; knockbackResistance = 0.1; }
        }

        if (armor > 0) meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(UUID.randomUUID(), "generic.armor", armor, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS));
        if (toughness > 0) meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(UUID.randomUUID(), "generic.armor_toughness", toughness, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS));
        if (knockbackResistance > 0) meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(UUID.randomUUID(), "generic.knockback_resistance", knockbackResistance, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS));
    }

    @Override
    public ISpecialAbility getSpecialAbility() {
        return new SuperJumpAbility();
    }
}
