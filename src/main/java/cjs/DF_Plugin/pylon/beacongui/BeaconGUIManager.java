package cjs.DF_Plugin.pylon.beacongui;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.pylon.beacongui.giftbox.GiftBoxGuiManager;
import cjs.DF_Plugin.pylon.beacongui.recruit.RecruitGuiManager;
import cjs.DF_Plugin.pylon.beacongui.resurrect.ResurrectGuiManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class BeaconGUIManager {

    private final DF_Main plugin;
    private final RecruitGuiManager recruitGuiManager;
    private final ResurrectGuiManager resurrectGuiManager;
    private final GiftBoxGuiManager giftBoxGuiManager;
    public static final String MAIN_GUI_TITLE = "§b[파일런 메뉴]";

    public BeaconGUIManager(DF_Main plugin) {
        this.plugin = plugin;
        this.recruitGuiManager = new RecruitGuiManager(plugin);
        this.resurrectGuiManager = new ResurrectGuiManager(plugin);
        this.giftBoxGuiManager = new GiftBoxGuiManager(plugin);
    }

    /**
     * 파일런 메인 메뉴 GUI를 엽니다.
     */
    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, MAIN_GUI_TITLE);

        // 아이템 생성 및 배치 (한 칸씩 오른쪽으로 이동)
        gui.setItem(10, createGuiItem(Material.TOTEM_OF_UNDYING, "§d팀원 부활", "resurrect", "§7사망한 팀원을 부활시킵니다."));
        gui.setItem(12, createGuiItem(Material.DIAMOND, "§a팀원 뽑기", "recruit", "§7새로운 팀원을 영입합니다."));
        gui.setItem(14, createGuiItem(Material.CHEST, "§e선물상자", "giftbox", "§7가문원에게 선물을 보냅니다."));
        gui.setItem(16, createGuiItem(Material.FIREWORK_ROCKET, "§c정찰용 폭죽", "recon_firework", "§7주변을 정찰할 수 있는 특수 폭죽을 받습니다."));
        gui.setItem(22, createGuiItem(Material.NETHER_STAR, "§b파일런 회수", "retrieve_pylon", "§7파일런을 회수하여 아이템으로 되돌립니다."));

        player.openInventory(gui);
    }

    /**
     * GUI에 사용될 아이템을 생성하는 헬퍼 메소드
     */
    private ItemStack createGuiItem(Material material, String name, String action, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            // 아이템에 어떤 버튼인지 식별자를 저장
            meta.getPersistentDataContainer().set(BeaconGUIListener.GUI_BUTTON_KEY, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleMenuClick(Player player, String action) {
        switch (action) {
            case "resurrect":
                resurrectGuiManager.openResurrectionGui(player);
                break;
            case "recruit":
                recruitGuiManager.startRecruitmentProcess(player);
                break;
            case "giftbox":
                giftBoxGuiManager.openGiftBox(player);
                break;
            case "recon_firework":
                plugin.getPylonManager().getReconManager().activateRecon(player);
                break;
            case "retrieve_pylon":
                plugin.getPylonManager().getRetrievalManager().retrievePylon(player);
                break;
        }
    }

    public RecruitGuiManager getRecruitGuiManager() { return recruitGuiManager; }
    public ResurrectGuiManager getResurrectGuiManager() { return resurrectGuiManager; }
    public GiftBoxGuiManager getGiftBoxGuiManager() { return giftBoxGuiManager; }
}