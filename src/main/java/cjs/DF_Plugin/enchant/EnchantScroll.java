package cjs.DF_Plugin.enchant;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class EnchantScroll {

    private static final NamespacedKey ENCHANT_SCROLL_KEY = new NamespacedKey(DF_Main.getInstance(), "enchant_scroll");

    /**
     * 마법부여 주문서 아이템을 생성합니다.
     * @param amount 생성할 개수
     * @return 생성된 마법부여 주문서 아이템
     */
    public static ItemStack createEnchantScroll(int amount) {
        ItemStack scroll = new ItemStack(Material.NETHER_STAR, amount);
        ItemMeta meta = scroll.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d§l고대 마력의 정수");
            // 기존 로어를 모두 제거하고 새로운 로어를 설정합니다.
            meta.setLore(Arrays.asList(
                    "§7고대 위더의 힘이 응축된 정수입니다.",
                    "§7이 아이템으로 특별한 마법을 부여할 수 있습니다."
            ));
            // 아이템을 빛나게 하는 효과 추가
            meta.addEnchant(Enchantment.LURE, 1, false);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            meta.getPersistentDataContainer().set(ENCHANT_SCROLL_KEY, PersistentDataType.BYTE, (byte) 1);
            scroll.setItemMeta(meta);
        }
        return scroll;
    }

    /**
     * 해당 아이템이 마법부여 주문서인지 확인합니다.
     */
    public static boolean isEnchantScroll(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(ENCHANT_SCROLL_KEY, PersistentDataType.BYTE);
    }
}