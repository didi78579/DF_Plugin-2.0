package cjs.DF_Plugin.offline;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 오프라인 플레이어의 인벤토리 GUI를 생성하고 관리하는 클래스.
 */
public class InventoryGUI {

    public static final int SIZE = 54; // 6x9 GUI
    public static final ItemStack FILLER_PANE;

    static {
        // GUI의 빈 공간을 채울 회색 유리판 아이템을 미리 생성합니다.
        FILLER_PANE = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = FILLER_PANE.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            FILLER_PANE.setItemMeta(meta);
        }
    }

    /**
     * 오프라인 플레이어의 인벤토리 정보로 GUI를 생성합니다.
     * @param offlineInventory 오프라인 플레이어의 인벤토리 데이터
     * @return 생성된 GUI
     */
    public static Inventory create(OfflineInventory offlineInventory) {
        Inventory gui = Bukkit.createInventory(null, SIZE, "오프라인 플레이어 인벤토리");

        // GUI 레이아웃 설정
        gui.setItem(0, offlineInventory.getPlayerHead());
        gui.setItem(2, offlineInventory.getArmor()[3]); // Helmet
        gui.setItem(3, offlineInventory.getArmor()[2]); // Chestplate
        gui.setItem(4, offlineInventory.getArmor()[1]); // Leggings
        gui.setItem(5, offlineInventory.getArmor()[0]); // Boots
        gui.setItem(8, offlineInventory.getOffHand());

        // 플레이어의 주 인벤토리 아이템들을 GUI에 배치 (18번 슬롯부터)
        ItemStack[] mainInv = offlineInventory.getMain();
        for (int i = 0; i < 36; i++) {
            if (i < mainInv.length && mainInv[i] != null) {
                gui.setItem(18 + i, mainInv[i]);
            }
        }

        // 장식용 슬롯을 유리판으로 채웁니다. 장비/인벤토리 칸은 비워둡니다.
        gui.setItem(1, FILLER_PANE);
        gui.setItem(6, FILLER_PANE);
        gui.setItem(7, FILLER_PANE);
        for (int i = 9; i <= 17; i++) {
            gui.setItem(i, FILLER_PANE);
        }

        return gui;
    }

    /**
     * GUI에서 아이템 정보를 다시 추출하여 OfflineInventory 객체로 변환합니다.
     * @param gui 닫힌 GUI
     * @return 추출된 인벤토리 데이터
     */
    public static OfflineInventory fromGui(Inventory gui) {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = gui.getItem(2); // Helmet
        armor[2] = gui.getItem(3); // Chestplate
        armor[1] = gui.getItem(4); // Leggings
        armor[0] = gui.getItem(5); // Boots

        ItemStack offHand = gui.getItem(8);

        ItemStack[] main = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            main[i] = gui.getItem(18 + i);
        }

        return new OfflineInventory(main, armor, offHand, gui.getItem(0));
    }
}