package cjs.DF_Plugin.pylon.item;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public final class PylonItem {

    private static NamespacedKey PYLON_ITEM_KEY;

    private PylonItem() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static NamespacedKey getKey() {
        if (PYLON_ITEM_KEY == null) {
            PYLON_ITEM_KEY = new NamespacedKey(DF_Main.getInstance(), "pylon_item");
        }
        return PYLON_ITEM_KEY;
    }

    public static ItemStack createPylonItem() {
        ItemStack pylon = new ItemStack(Material.BEACON);
        ItemMeta meta = pylon.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§b§l[ 파일런 ]");
            meta.setLore(Arrays.asList(
                    "§7가문의 중심이 되는 신성한 신호기입니다.",
                    "§c버리거나 상자에 보관할 수 없습니다."
            ));
            meta.getPersistentDataContainer().set(getKey(), PersistentDataType.BYTE, (byte) 1);
            pylon.setItemMeta(meta);
        }
        return pylon;
    }

    public static boolean isPylonItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(getKey(), PersistentDataType.BYTE);
    }
}