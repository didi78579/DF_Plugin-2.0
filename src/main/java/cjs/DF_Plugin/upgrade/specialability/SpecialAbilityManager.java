package cjs.DF_Plugin.upgrade.specialability;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.profile.IWeaponProfile;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpecialAbilityManager {

    public static final NamespacedKey SPECIAL_ABILITY_KEY = new NamespacedKey(DF_Main.getInstance(), "special_ability");
    public static final NamespacedKey ITEM_UUID_KEY = new NamespacedKey(DF_Main.getInstance(), "item_uuid");

    private final DF_Main plugin;
    // Player UUID -> (Ability Key -> Cooldown Info)
    private final Map<UUID, Map<String, CooldownInfo>> playerCooldowns;
    // Player UUID -> (Ability Key -> Charge Info)
    private final Map<UUID, Map<String, ChargeInfo>> playerCharges;

    // Record to hold cooldown information for the action bar
    public record CooldownInfo(long endTime, String displayName) {}
    // Record to hold charge information for the action bar
    public record ChargeInfo(int current, int max, String displayName) {}

    public SpecialAbilityManager(DF_Main plugin, Map<UUID, Map<String, CooldownInfo>> initialCooldowns, Map<UUID, Map<String, ChargeInfo>> initialCharges) {
        this.plugin = plugin;
        this.playerCooldowns = initialCooldowns;
        this.playerCharges = initialCharges;
    }

    public Map<UUID, Map<String, CooldownInfo>> getCooldownsMap() {
        return playerCooldowns;
    }

    public Map<UUID, Map<String, ChargeInfo>> getChargesMap() {
        return playerCharges;
    }

    public boolean isAbilityOnCooldown(Player player, ISpecialAbility ability, ItemStack item) {
        String cooldownKey = getCooldownKey(player, ability, item);
        Map<String, CooldownInfo> cooldowns = playerCooldowns.get(player.getUniqueId());

        if (cooldowns != null && cooldowns.containsKey(cooldownKey)) {
            return System.currentTimeMillis() < cooldowns.get(cooldownKey).endTime();
        }
        return false; // Not on cooldown
    }

    public void setCooldown(Player player, ISpecialAbility ability, ItemStack item) {
        setCooldown(player, ability, item, ability.getCooldown());
    }

    /**
     * Sets a cooldown for a specific ability with a custom duration.
     * @param player The player to set the cooldown for.
     * @param ability The ability to set the cooldown for.
     * @param item The item associated with the ability.
     * @param cooldownSeconds The duration of the cooldown in seconds.
     */
    public void setCooldown(Player player, ISpecialAbility ability, ItemStack item, double cooldownSeconds) {
        if (cooldownSeconds <= 0) return;

        long newEndTime = System.currentTimeMillis() + (long) (cooldownSeconds * 1000);
        String cooldownKey = getCooldownKey(player, ability, item);
        String displayName = ability.getDisplayName();

        Map<String, CooldownInfo> cooldowns = playerCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());

        // '공중 대쉬' 능력은 피격 시마다 쿨다운을 항상 초기화합니다.
        if (ability.getInternalName().equals("double_jump")) {
            cooldowns.put(cooldownKey, new CooldownInfo(newEndTime, displayName));
            return;
        }

        // 기존 쿨다운이 존재하고, 그 남은 시간이 새로 적용할 쿨다운보다 길다면, 기존 쿨다운을 유지합니다.
        // 이렇게 하면 짧은 쿨다운이 긴 쿨다운을 덮어쓰는 것을 방지합니다.
        CooldownInfo existingCooldown = cooldowns.get(cooldownKey);
        if (existingCooldown != null && existingCooldown.endTime() > newEndTime) {
            return; // 기존 쿨다운이 더 길므로 아무것도 하지 않음
        }

        // 새 쿨다운을 적용합니다.
        cooldowns.put(cooldownKey, new CooldownInfo(newEndTime, displayName));
    }

    public long getRemainingCooldown(Player player, ISpecialAbility ability, ItemStack item) {
        String cooldownKey = getCooldownKey(player, ability, item);
        Map<String, CooldownInfo> cooldowns = playerCooldowns.get(player.getUniqueId());

        if (cooldowns != null && cooldowns.containsKey(cooldownKey)) {
            long endTime = cooldowns.get(cooldownKey).endTime();
            return Math.max(0, endTime - System.currentTimeMillis());
        }
        return 0;
    }

    public void setChargeInfo(Player player, ISpecialAbility ability, int current, int max) {
        String chargeKey = getChargeKey(ability);
        String displayName = ability.getDisplayName();
        playerCharges.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(chargeKey, new ChargeInfo(current, max, displayName));
    }

    public ChargeInfo getChargeInfo(Player player, ISpecialAbility ability) {
        Map<String, ChargeInfo> charges = playerCharges.get(player.getUniqueId());
        if (charges != null) {
            return charges.get(getChargeKey(ability));
        }
        return null;
    }

    public void removeChargeInfo(Player player, ISpecialAbility ability) {
        Map<String, ChargeInfo> charges = playerCharges.get(player.getUniqueId());
        if (charges != null) {
            charges.remove(getChargeKey(ability));
            if (charges.isEmpty()) {
                playerCharges.remove(player.getUniqueId());
            }
        }
    }

    private String getChargeKey(ISpecialAbility ability) {
        return ability.getInternalName();
    }

    private String getCooldownKey(Player player, ISpecialAbility ability, ItemStack item) {
        // This logic can be configured to have cooldowns per weapon or per ability type
        boolean perItemCooldown = plugin.getUpgradeSettingManager().getConfig().getBoolean("cooldown.per-weapon-cooldown", true);
        if (perItemCooldown && item != null && item.hasItemMeta()) {
            // 아이템의 hashCode 대신, 아이템에 저장된 영구적인 UUID를 사용하여 키를 생성합니다.
            // 이것이 쿨다운 추적 버그를 해결하는 핵심입니다.
            String itemUUID = item.getItemMeta().getPersistentDataContainer().get(ITEM_UUID_KEY, PersistentDataType.STRING);
            if (itemUUID != null) {
                return ability.getInternalName() + ":" + itemUUID;
            }
        }
        return ability.getInternalName();
    }

    public Map<String, CooldownInfo> getPlayerCooldowns(UUID playerUUID) {
        return playerCooldowns.get(playerUUID);
    }

    public Map<String, ChargeInfo> getPlayerCharges(UUID playerUUID) {
        return playerCharges.get(playerUUID);
    }

    public ISpecialAbility getAbilityFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String abilityKey = item.getItemMeta().getPersistentDataContainer().get(SPECIAL_ABILITY_KEY, PersistentDataType.STRING);
        if (abilityKey == null) return null;

        IWeaponProfile profile = plugin.getWeaponProfileManager().getProfile(item.getType());
        return (profile != null && profile.getSpecialAbility() != null && profile.getSpecialAbility().getInternalName().equals(abilityKey)) ? profile.getSpecialAbility() : null;
    }
}