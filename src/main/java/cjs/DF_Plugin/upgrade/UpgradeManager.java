package cjs.DF_Plugin.upgrade;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.items.UpgradeItems;
import cjs.DF_Plugin.upgrade.profile.IWeaponProfile;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class UpgradeManager {
    private final DF_Main plugin;
    private final Random random = new Random();
    private static final int MAX_UPGRADE_LEVEL = 10;

    public UpgradeManager(DF_Main plugin) {
        this.plugin = plugin;
    }

    public int getUpgradeLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                // 별이 포함된 라인을 찾습니다.
                if (line.contains("★") || line.contains("☆")) {
                    int level = 0;
                    // 색상 코드를 제거하고 채워진 별의 개수를 셉니다.
                    for (char c : ChatColor.stripColor(line).toCharArray()) {
                        if (c == '★') {
                            level++;
                        }
                    }
                    return level;
                }
            }
        }
        return 0;
    }

    public void attemptUpgrade(Player player, ItemStack item) {
        // 1. 강화 대상 분석
        IWeaponProfile profile = plugin.getWeaponProfileManager().getProfile(item.getType());
        if (profile == null) {
            player.sendMessage("§c이 아이템은 강화할 수 없습니다.");
            return;
        }

        // 3. 강화 정보 불러오기 (레벨)
        final int currentLevel = getUpgradeLevel(item);

        // 2. 강화 비용 확인
        int requiredStones = currentLevel + 1;
        if (!hasEnoughStones(player, requiredStones)) {
            player.sendMessage(ChatColor.RED + "강화석이 부족합니다! (필요: " + requiredStones + "개)");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.8f);
            return;
        }

        if (currentLevel >= MAX_UPGRADE_LEVEL) {
            player.sendMessage("§c최대 강화 레벨에 도달했습니다.");
            return;
        }

        // 4. 강화 실행
        consumeStones(player, requiredStones); // 강화석 소모
        FileConfiguration config = plugin.getUpgradeSettingManager().getConfig();
        String path = "level-settings." + currentLevel;

        if (!config.isConfigurationSection(path)) {
            player.sendMessage("§c다음 강화 레벨에 대한 설정이 없습니다. (레벨: " + currentLevel + ")");
            // 설정이 없으면 강화석 환불
            player.getInventory().addItem(UpgradeItems.createUpgradeStone(requiredStones));
            return;
        }

        double successChance = config.getDouble(path + ".success", 0.0);
        double failureChance = config.getDouble(path + ".failure", 0.0);
        double downgradeChance = config.getDouble(path + ".downgrade", 0.0);
        // 파괴 확률은 나머지입니다.

        double roll = random.nextDouble(); // 0.0 이상 1.0 미만

        if (roll < successChance) {
            // 성공
            int newLevel = currentLevel + 1;
            setUpgradeLevel(item, profile, newLevel);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.5f);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            // 10강 달성 시 전설 알림
            if (newLevel == MAX_UPGRADE_LEVEL) {
                handleLegendaryUpgrade(player, item);
            }
        } else if (roll < successChance + failureChance) {
            // 실패 (레벨 유지)
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.05);
        } else if (roll < successChance + failureChance + downgradeChance) {
            // 하락
            int newLevel = Math.max(0, currentLevel - 1);
            setUpgradeLevel(item, profile, newLevel);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.5f);
            player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
        } else {
            // 파괴
            String itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
            Bukkit.broadcastMessage("§c[!] §e" + player.getName() + "§7님이 §f" + itemName + " §c(+" + currentLevel + ")§7 강화에 실패하여 아이템이 파괴되었습니다.");
            item.setAmount(0);
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.LAVA, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
        }
    }

    public void setUpgradeLevel(ItemStack item, IWeaponProfile profile, int newLevel) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 레벨에 따라 아이템 이름 처리 (전설의 접두사)
        if (newLevel >= MAX_UPGRADE_LEVEL) {
            String baseName = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : item.getType().name().toLowerCase().replace('_', ' ');
            if (baseName.startsWith("전설의 ")) {
                // 이미 접두사가 있는 경우, 색상만 확실히 금색으로 설정
                meta.setDisplayName("§6" + baseName);
            } else {
                // 새로운 전설 아이템
                meta.setDisplayName("§6전설의 " + baseName);
            }
        } else {
            // 10강 미만일 경우, 전설의 접두사 제거
            if (meta.hasDisplayName()) {
                String strippedName = ChatColor.stripColor(meta.getDisplayName());
                if (strippedName.startsWith("전설의 ")) {
                    meta.setDisplayName(strippedName.substring("전설의 ".length()).trim());
                }
            }
        }

        List<String> lore = new ArrayList<>();

        // 1. 새로운 별 표시 추가
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < newLevel; i++) {
            stars.append(ChatColor.GOLD).append("★");
        }
        for (int i = newLevel; i < MAX_UPGRADE_LEVEL; i++) {
            stars.append(ChatColor.GRAY).append("☆");
        }
        lore.add(stars.toString().trim());
        lore.add(""); // 간격을 위한 빈 줄

        // 2. 다음 레벨의 확률 정보 추가
        FileConfiguration config = plugin.getUpgradeSettingManager().getConfig();
        if (!config.getBoolean("show-success-chance", true)) {
            // 설정이 꺼져있으면 아무것도 하지 않음
        } else if (newLevel >= MAX_UPGRADE_LEVEL) {
            // 아이템이 이미 최대 레벨에 도달한 경우
            lore.add(ChatColor.GOLD + "최대 강화 레벨에 도달했습니다!");
        } else {
            // 다음 강화 레벨에 대한 정보를 yml에서 찾습니다.
            String path = "level-settings." + newLevel;
            if (config.isConfigurationSection(path)) {
                double success = config.getDouble(path + ".success", 0.0) * 100;
                double failure = config.getDouble(path + ".failure", 0.0) * 100;
                double downgrade = config.getDouble(path + ".downgrade", 0.0) * 100;
                double destroy = config.getDouble(path + ".destroy", 0.0) * 100;

                lore.add(ChatColor.GREEN + "성공 확률: " + String.format("%.1f", success) + "%");
                lore.add(ChatColor.YELLOW + "실패(유지) 확률: " + String.format("%.1f", failure) + "%");
                lore.add(ChatColor.RED + "하락 확률: " + String.format("%.1f", downgrade) + "%");
                lore.add(ChatColor.DARK_RED + "파괴 확률: " + String.format("%.1f", destroy) + "%");
            } else {
                lore.add(ChatColor.GRAY + "다음 강화 정보가 없습니다.");
            }
        }

        // 4. 특수 능력 표시 처리
        ISpecialAbility ability = profile.getSpecialAbility();
        if (ability != null) {
            if (newLevel >= MAX_UPGRADE_LEVEL) {
                lore.add(""); // 간격을 위한 빈 줄
                String abilityName = ChatColor.stripColor(ability.getDisplayName());
                lore.add("§f[§b특수능력§f] : §b" + abilityName);
                lore.add(ability.getDescription());
                // 아이템에 고유 ID가 없으면 부여하여 쿨다운 추적에 사용합니다.
                if (!meta.getPersistentDataContainer().has(SpecialAbilityManager.ITEM_UUID_KEY, PersistentDataType.STRING)) {
                    meta.getPersistentDataContainer().set(SpecialAbilityManager.ITEM_UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
                }
                meta.getPersistentDataContainer().set(SpecialAbilityManager.SPECIAL_ABILITY_KEY, PersistentDataType.STRING, ability.getInternalName());
            } else {
                meta.getPersistentDataContainer().remove(SpecialAbilityManager.SPECIAL_ABILITY_KEY);
            }
        }

        // 3. 스탯 속성 적용
        profile.applyAttributes(item, meta, newLevel, lore);

        // 5. 최종 적용
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private void handleLegendaryUpgrade(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String legendaryName = meta.getDisplayName();

        // 모든 플레이어에게 메시지 전송 및 소리 재생
        String message = "§6[!] " + player.getName() + "님에 의해 " + legendaryName + "§6이(가) 탄생했습니다!";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(message);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    private boolean hasEnoughStones(Player player, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (UpgradeItems.isUpgradeStone(item)) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private void consumeStones(Player player, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (remaining <= 0) break;
            if (UpgradeItems.isUpgradeStone(item)) {
                int toTake = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - toTake);
                remaining -= toTake;
            }
        }
    }

    public static void applyCyclingEnchantments(ItemMeta meta, int level, Map<Enchantment, Double> enchantBonuses) {
        if (enchantBonuses == null || enchantBonuses.isEmpty()) {
            return;
        }

        // 1. 이 시스템으로 관리되는 모든 인챈트를 먼저 제거하여 상태를 초기화합니다.
        // 이렇게 하지 않으면 레벨이 하락했을 때 높은 레벨의 인챈트가 남을 수 있습니다.
        for (Enchantment ench : enchantBonuses.keySet()) {
            meta.removeEnchant(ench);
        }

        // 2. 레벨이 0 이하라면 여기서 작업을 마칩니다.
        if (level <= 0) {
            return;
        }

        // 3. 강화 레벨에 따라 로직 분기
        if (level >= MAX_UPGRADE_LEVEL) {
            // 10강: 모든 인챈트 레벨을 설정값에 따라 계산하여 동시에 적용
            for (Map.Entry<Enchantment, Double> entry : enchantBonuses.entrySet()) {
                Enchantment enchantment = entry.getKey();
                double bonusPerLevel = entry.getValue();
                // 10강에서는 per-level 보너스를 모두 합산하여 적용
                int enchantmentLevel = (int) Math.floor(MAX_UPGRADE_LEVEL * bonusPerLevel);

                if (enchantmentLevel > 0) {
                    meta.addEnchant(enchantment, enchantmentLevel, true);
                }
            }
        } else {
            // 1-9강: 인챈트를 번갈아 가며 적용
            List<Enchantment> cycleOrder = new ArrayList<>(enchantBonuses.keySet());
            if (!cycleOrder.isEmpty()) {
                Map<Enchantment, Integer> levelsToAdd = new HashMap<>();
                int numEnchants = cycleOrder.size();

                // 먼저 각 인챈트가 몇 레벨이 되어야 하는지 계산합니다.
                for (int i = 1; i <= level; i++) {
                    Enchantment targetEnchant = cycleOrder.get((i - 1) % numEnchants);
                    levelsToAdd.put(targetEnchant, levelsToAdd.getOrDefault(targetEnchant, 0) + 1);
                }

                // 계산된 레벨을 한 번에 적용합니다.
                levelsToAdd.forEach((enchant, enchantLevel) -> meta.addEnchant(enchant, enchantLevel, true));
            }
        }
    }
}