package cjs.DF_Plugin.listener;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerMoveListener implements Listener {
    private final DF_Main plugin;
    private final SpecialAbilityManager specialAbilityManager;

    public PlayerMoveListener(DF_Main plugin) {
        this.plugin = plugin;
        this.specialAbilityManager = plugin.getSpecialAbilityManager();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // 이중 도약 능력의 비행 활성화/비활성화 처리
        ItemStack boots = player.getInventory().getBoots();
        ISpecialAbility bootsAbility = specialAbilityManager.getAbilityFromItem(boots);

        if (bootsAbility != null && bootsAbility.getInternalName().equals("double_jump")) {
            if (player.isOnGround() && !player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
        } else {
            // 이중 도약 부츠를 신고 있지 않으면 비행을 비활성화 (크리에이티브/관전 모드 제외)
            if (player.getAllowFlight() && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
            }
        }

        // 이동 시 발동하는 모든 장비의 특수 능력 처리 (예: 재생)
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            ISpecialAbility moveAbility = specialAbilityManager.getAbilityFromItem(armor);
            if (moveAbility != null && !specialAbilityManager.isAbilityOnCooldown(player, moveAbility, armor)) {
                moveAbility.onPlayerMove(event, player, armor);
            }
        }
    }
}