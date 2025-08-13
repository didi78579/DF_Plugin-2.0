package cjs.DF_Plugin.items;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MasterCompass {

    public static final NamespacedKey MASTER_COMPASS_KEY = new NamespacedKey(DF_Main.getInstance(), "master_compass");

    public static ItemStack createMasterCompass() {
        return new ItemBuilder(Material.COMPASS)
                .withName("§5마스터 컴퍼스")
                .withLore(
                        "§7고대 용의 눈으로 만들어진 나침반입니다.",
                        "§7우클릭 시 가장 가까운 적의 파일런을 향합니다."
                )
                .withPDCString(MASTER_COMPASS_KEY, "true")
                .build();
    }

    public static boolean isMasterCompass(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(MASTER_COMPASS_KEY, PersistentDataType.STRING);
    }
}