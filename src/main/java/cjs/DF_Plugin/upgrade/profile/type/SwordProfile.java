package cjs.DF_Plugin.upgrade.profile.type;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.UpgradeManager;
import cjs.DF_Plugin.upgrade.profile.IUpgradeableProfile;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.impl.SwordDanceAbility;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SwordProfile implements IUpgradeableProfile {

    private static final ISpecialAbility SWORD_DANCE_ABILITY = new SwordDanceAbility();
    private static final UUID ATTACK_DAMAGE_MODIFIER_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID BASE_ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA2");

    @Override
    public void applyAttributes(ItemStack item, ItemMeta meta, int level) {
        // 1. 기존 공격 관련 속성을 모두 제거합니다.
        meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);

        // --- 실제 속성 적용 (내부적으로만) ---
        // 2. 공격 피해 속성을 적용합니다.
        double damageModifierValue = getBaseDamageModifier(item.getType());
        if (damageModifierValue > 0) {
            AttributeModifier damageModifier = new AttributeModifier(ATTACK_DAMAGE_MODIFIER_UUID, "weapon.damage", damageModifierValue, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, damageModifier);
        }

        // 3. 공격 속도 속성을 적용합니다.
        AttributeModifier speedModifier;
        if (level >= 10) {
            // 10강 이상일 경우: 공격 속도를 최대치(사실상 무한)로 설정합니다.
            // 바닐라 기본값 4.0에 1020을 더해 1024로 만듭니다. 이 값은 공격 쿨다운이 없는 것처럼 보이게 합니다.
            // 실제 즉시 공격 로직은 SwordDanceAbility에서 처리합니다.
            speedModifier = new AttributeModifier(BASE_ATTACK_SPEED_MODIFIER_UUID, "weapon.attack_speed", 1020, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
        } else {
            // 10강 미만일 경우: 레벨에 따라 점진적으로 공격 속도를 증가시킵니다.
            final double baseAttackSpeedAttribute = -2.4; // 4.0 (base) - 2.4 = 1.6
            double speedBonusPerLevel = DF_Main.getInstance().getGameConfigManager().getConfig()
                    .getDouble("upgrade.generic-bonuses.sword.attack-speed-per-level", 0.3);
            double totalBonus = speedBonusPerLevel * level;
            double finalAttackSpeedModifierValue = baseAttackSpeedAttribute + totalBonus;
            speedModifier = new AttributeModifier(BASE_ATTACK_SPEED_MODIFIER_UUID, "weapon.attack_speed", finalAttackSpeedModifierValue, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
        }
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, speedModifier);

        // --- 로어 표시 수정 ---
        // 4. 기본 속성 표시(녹색 줄)를 숨깁니다.
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }

    @Override
    public List<String> getPassiveBonusLore(ItemStack item, int level) {
        if (level <= 0) {
            return Collections.emptyList();
        }
        if (level >= 10) {
            return List.of("§b공격 속도: 최대");
        }
        double speedBonusPerLevel = DF_Main.getInstance().getGameConfigManager().getConfig()
                .getDouble("upgrade.generic-bonuses.sword.attack-speed-per-level", 0.3);
        double totalBonus = speedBonusPerLevel * level;
        return List.of("§b추가 공격속도: +" + String.format("%.1f", totalBonus));
    }

    @Override
    public List<String> getBaseStatsLore(ItemStack item, int level) {
        List<String> baseLore = new ArrayList<>();
        baseLore.add("§7주로 사용하는 손에 있을 때:");

        // 최종 공격 피해 계산 및 추가
        double damageModifierValue = getBaseDamageModifier(item.getType());
        double finalDamage = 1.0 + damageModifierValue;
        baseLore.add("§2 " + String.format("%.1f", finalDamage) + " 공격 피해");

        if (level >= 10) {
            // 10강 이상에서는 공격 속도 정보가 getPassiveBonusLore에서 표시되므로 여기서는 추가하지 않습니다.
        } else {
            // 10강 미만일 경우 실제 수치 계산 및 추가
            final double baseAttackSpeedAttribute = -2.4;
            double speedBonusPerLevel = DF_Main.getInstance().getGameConfigManager().getConfig()
                    .getDouble("upgrade.generic-bonuses.sword.attack-speed-per-level", 0.3);
            double totalBonus = speedBonusPerLevel * level;
            double finalAttackSpeedModifierValue = baseAttackSpeedAttribute + totalBonus;
            double finalSpeed = 4.0 + finalAttackSpeedModifierValue; // 4.0 + (-2.4 + bonus) = 1.6 + bonus
            baseLore.add("§2 " + String.format("%.1f", finalSpeed) + " 공격 속도");
        }

        return baseLore;
    }

    private double getBaseDamageModifier(Material material) {
        return switch (material) {
            case WOODEN_SWORD, GOLDEN_SWORD -> 3.0;
            case STONE_SWORD -> 4.0;
            case IRON_SWORD -> 5.0;
            case DIAMOND_SWORD -> 6.0;
            case NETHERITE_SWORD -> 7.0;
            default -> 0.0;
        };
    }

    @Override
    public Optional<ISpecialAbility> getSpecialAbility() {
        return Optional.of(SWORD_DANCE_ABILITY);
    }
}