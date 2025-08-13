package cjs.DF_Plugin.enchant;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EnchantManager {

    private final DF_Main plugin;

    public EnchantManager(DF_Main plugin) {
        this.plugin = plugin;
    }

    public void attemptEnchant(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (hasUpgradeStars(meta)) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.8f);
            player.sendMessage(ChatColor.RED + "이 아이템은 이미 강화되었습니다. 더 이상 인챈트를 할 수 없습니다.");
            return;
        }

        if (!hasMagicStone(player)) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.8f);
            player.sendMessage(ChatColor.RED + "마석이 부족합니다.");
            return;
        }

        consumeMagicStone(player);
        enchantItem(item);

        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        player.sendMessage(ChatColor.GREEN + "아이템에 새로운 마법이 깃들었습니다!");
    }

    private void enchantItem(ItemStack item) {
        Random random = new Random();
        double extraEnchantChance = 0.10; // 10% 추가 인챈트 확률
        double curseChance = 0.01; // 저주 확률 1%
        int maxEnchantments = 8; // 최대 인챈트 수

        // 제외할 인챈트 목록
        List<Enchantment> excludedEnchants = Arrays.asList(
                Enchantment.BINDING_CURSE,
                Enchantment.VANISHING_CURSE,
                Enchantment.THORNS
        );

        // 기존 인챈트 모두 제거
        item.getEnchantments().keySet().forEach(item::removeEnchantment);

        Map<Enchantment, Integer> newEnchantments = new HashMap<>();
        int enchantmentCount = 0;

        // 최소 1개의 인챈트 보장
        addRandomEnchant(newEnchantments, excludedEnchants, random);
        enchantmentCount++;

        // 추가 인챈트 부여
        while (enchantmentCount < maxEnchantments && random.nextDouble() < extraEnchantChance) {
            addRandomEnchant(newEnchantments, excludedEnchants, random);
            enchantmentCount++;
        }

        // 저주 추가
        if (random.nextDouble() < curseChance) {
            newEnchantments.put(Enchantment.BINDING_CURSE, 1);
        }
        if (random.nextDouble() < curseChance) {
            newEnchantments.put(Enchantment.VANISHING_CURSE, 1);
        }

        // 아이템에 최종 적용
        item.addUnsafeEnchantments(newEnchantments);
    }

    private void addRandomEnchant(Map<Enchantment, Integer> currentEnchants, List<Enchantment> excluded, Random random) {
        Enchantment randomEnchant;
        do {
            randomEnchant = Enchantment.values()[random.nextInt(Enchantment.values().length)];
        } while (excluded.contains(randomEnchant) || currentEnchants.containsKey(randomEnchant) || !randomEnchant.canEnchantItem(new ItemStack(Material.DIAMOND_SWORD))); // 아이템 종류에 맞는 인챈트만

        int level = random.nextInt(randomEnchant.getMaxLevel()) + 1;
        currentEnchants.put(randomEnchant, level);
    }

    private boolean hasMagicStone(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (EnchantScroll.isEnchantScroll(item)) {
                return true;
            }
        }
        return false;
    }

    private void consumeMagicStone(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (EnchantScroll.isEnchantScroll(item)) {
                item.setAmount(item.getAmount() - 1);
                return;
            }
        }
    }

    private boolean hasUpgradeStars(ItemMeta meta) {
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.contains("§6★") || line.contains("§7☆")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}