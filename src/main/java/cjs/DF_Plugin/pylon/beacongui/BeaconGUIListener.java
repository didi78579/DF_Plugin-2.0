package cjs.DF_Plugin.pylon.beacongui;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.util.PluginUtils;
import cjs.DF_Plugin.pylon.beacongui.recruit.RecruitGuiManager;
import cjs.DF_Plugin.pylon.beacongui.resurrect.ResurrectGuiManager;
import org.bukkit.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class BeaconGUIListener implements Listener {

    public static NamespacedKey GUI_BUTTON_KEY;
    public static NamespacedKey PYLON_ITEM_KEY;
    private final DF_Main plugin;
    private final BeaconGUIManager guiManager;

    public BeaconGUIListener(DF_Main plugin, BeaconGUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        GUI_BUTTON_KEY = new NamespacedKey(plugin, "gui_button_action");
        PYLON_ITEM_KEY = new NamespacedKey(plugin, "pylon_core_item");
    }

    @EventHandler
    public void onPylonInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.BEACON) return;

        Player player = event.getPlayer();
        Clan playerClan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (playerClan == null) return;

        String clickedLocationStr = PluginUtils.serializeLocation(clickedBlock.getLocation());
        if (playerClan.getPylonLocations().contains(clickedLocationStr)) {
            event.setCancelled(true);
            guiManager.openMainMenu(player);
        } else {
            // The beacon is not a pylon yet. Check if the player is trying to create one.
            ItemStack itemInHand = event.getItem();
            // 파일런 생성 아이템(예: 특정 태그가 붙은 네더의 별)을 들고 있는지 확인합니다.
            if (itemInHand != null && itemInHand.getType() == Material.NETHER_STAR && itemInHand.hasItemMeta()) {
                if (itemInHand.getItemMeta().getPersistentDataContainer().has(PYLON_ITEM_KEY, PersistentDataType.BYTE)) {
                    // 파일런 생성 아이템이 맞으므로, 새로운 파일런을 생성합니다.
                    event.setCancelled(true);
                    playerClan.getPylonLocations().add(clickedLocationStr);
                    // 참고: 클랜 데이터가 나중에 저장된다고 가정합니다. 즉시 저장을 위해서는 ClanManager에 저장 메소드 호출이 필요합니다.
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                    player.sendMessage("§a새로운 파일런을 성공적으로 설치했습니다!");
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
                }
            }
        }
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        final String title = event.getView().getTitle();
        final boolean isMainGUI = title.equals(BeaconGUIManager.MAIN_GUI_TITLE);
        final boolean isRecruitGUI = title.equals(RecruitGuiManager.RECRUIT_GUI_TITLE);
        final boolean isResurrectGUI = title.equals(ResurrectGuiManager.RESURRECT_GUI_TITLE);

        // 플러그인의 GUI가 아니면 아무것도 하지 않음
        if (!isMainGUI && !isRecruitGUI && !isResurrectGUI) {
            return;
        }

        // [핵심 수정] 클릭된 인벤토리가 GUI(상단 인벤토리)인지 확인합니다.
        // 이 조건문 덕분에 플레이어 자신의 인벤토리(하단) 클릭은 영향을 받지 않습니다.
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            // GUI 내부에서의 클릭이므로, 아이템을 가져가는 것을 막기 위해 이벤트를 취소합니다.
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            if (isMainGUI) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || !clickedItem.hasItemMeta()) return;

                ItemMeta meta = clickedItem.getItemMeta();
                if (meta.getPersistentDataContainer().has(GUI_BUTTON_KEY, PersistentDataType.STRING)) {
                    String action = meta.getPersistentDataContainer().get(GUI_BUTTON_KEY, PersistentDataType.STRING);
                    guiManager.handleMenuClick(player, action);
                }
            } else if (isRecruitGUI) {
                guiManager.getRecruitGuiManager().handleGuiClick(event);
            } else if (isResurrectGUI) {
                guiManager.getResurrectGuiManager().handleGuiClick(event);
            }
        }
    }
}