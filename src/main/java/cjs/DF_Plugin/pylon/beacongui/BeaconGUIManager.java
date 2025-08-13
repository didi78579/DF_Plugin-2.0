package cjs.DF_Plugin.pylon.beacongui;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.pylon.beacongui.giftbox.GiftBoxGuiManager;
import cjs.DF_Plugin.pylon.beacongui.shop.PylonShopManager;
import cjs.DF_Plugin.settings.GameConfigManager;
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
    private final PylonShopManager shopManager;
    public static final String MAIN_GUI_TITLE = "§b[파일런 메뉴]";

    public BeaconGUIManager(DF_Main plugin) {
        this.plugin = plugin;
        this.recruitGuiManager = new RecruitGuiManager(plugin);
        this.resurrectGuiManager = new ResurrectGuiManager(plugin);
        this.giftBoxGuiManager = new GiftBoxGuiManager(plugin);
        this.shopManager = new PylonShopManager(plugin);
    }

    /**
     * 파일런 메인 메뉴 GUI를 엽니다.
     */
    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, MAIN_GUI_TITLE);

        // 아이템 생성 및 배치
        gui.setItem(10, createGuiItem(Material.ENDER_CHEST, "§e개인 창고", "personal_storage", "§7가문원과 공유하지 않는 개인 창고를 엽니다."));
        gui.setItem(11, createGuiItem(Material.OBSIDIAN, "§5클랜 지옥", "clan_nether", "§7가문 전용 지옥 공간으로 이동합니다."));
        gui.setItem(12, createGuiItem(Material.TOTEM_OF_UNDYING, "§d팀원 부활", "resurrect", "§7사망한 팀원을 부활시킵니다."));
        gui.setItem(13, createGuiItem(Material.DIAMOND, "§a팀원 뽑기", "recruit", "§7새로운 팀원을 영입합니다."));
        gui.setItem(14, createGuiItem(Material.CHEST, "§e선물상자", "giftbox", "§7가문원에게 선물을 보냅니다."));
        gui.setItem(15, createGuiItem(Material.FIREWORK_ROCKET, "§c정찰용 폭죽", "recon_firework", "§7주변을 정찰할 수 있는 특수 폭죽을 받습니다."));
        gui.setItem(16, createGuiItem(Material.NETHER_STAR, "§b파일런 회수", "retrieve_pylon", "§7현재 파일런을 회수하여 아이템으로 되돌립니다."));
        gui.setItem(22, createGuiItem(Material.GOLD_INGOT, "§6파일런 상점", "shop", "§7다양한 아이템을 구매하거나 교환합니다."));

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
        GameConfigManager configManager = plugin.getGameConfigManager();

        switch (action) {
            case "personal_storage":
                if (!configManager.getConfig().getBoolean("pylon-features.storage", true)) {
                    player.sendMessage("§c개인 창고 기능이 비활성화되어 있습니다.");
                    player.closeInventory();
                    return;
                }
                // TODO: 개인 창고를 여는 로직 구현
                player.sendMessage("§a개인 창고를 엽니다. (구현 예정)");
                player.closeInventory();
                break;
            case "clan_nether":
                if (!configManager.getConfig().getBoolean("pylon-features.clan-nether", true)) {
                    player.sendMessage("§c클랜 지옥 기능이 비활성화되어 있습니다.");
                    player.closeInventory();
                    return;
                }
                // TODO: 클랜 지옥으로 텔레포트하는 로직 구현
                player.sendMessage("§a클랜 지옥으로 이동합니다. (구현 예정)");
                player.closeInventory();
                break;
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
            case "shop":
                shopManager.openShopGui(player);
                break;
        }
    }

    public RecruitGuiManager getRecruitGuiManager() { return recruitGuiManager; }
    public ResurrectGuiManager getResurrectGuiManager() { return resurrectGuiManager; }
    public GiftBoxGuiManager getGiftBoxGuiManager() { return giftBoxGuiManager; }
    public PylonShopManager getShopManager() {
        return shopManager;
    }
}