package cjs.DF_Plugin.pylon.item;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class PylonItemFactory {

    public static final NamespacedKey AUXILIARY_CORE_KEY = new NamespacedKey(DF_Main.getInstance(), "auxiliary_pylon_core");
    public static final NamespacedKey RETURN_SCROLL_KEY = new NamespacedKey(DF_Main.getInstance(), "return_scroll");

    /**
     * 보조 파일런 코어 아이템을 생성합니다.
     * @return 보조 파일런 코어 ItemStack
     */
    public static ItemStack createAuxiliaryCore() {
        ItemStack core = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = core.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d보조 파일런 코어");
            meta.setLore(Arrays.asList("§7주 파일런의 신호기 범위 내에 설치하여", "§7가문의 영역을 확장할 수 있습니다."));
            meta.getPersistentDataContainer().set(AUXILIARY_CORE_KEY, PersistentDataType.BYTE, (byte) 1);
            core.setItemMeta(meta);
        }
        return core;
    }

    /**
     * 해당 아이템이 보조 파일런 코어인지 확인합니다.
     * @param item 확인할 ItemStack
     * @return 보조 파일런 코어가 맞으면 true
     */
    public static boolean isAuxiliaryCore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(AUXILIARY_CORE_KEY, PersistentDataType.BYTE);
    }

    // TODO: 귀환 주문서 생성 및 확인 메소드 추가

}