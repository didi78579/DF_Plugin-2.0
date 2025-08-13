package cjs.DF_Plugin.events.supplydrop;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class AltarInteractionListener implements Listener {

    private final SupplyDropManager supplyDropManager;

    public AltarInteractionListener(DF_Main plugin) {
        this.supplyDropManager = plugin.getSupplyDropManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!supplyDropManager.isAltarBlock(block.getLocation())) return;

        event.setCancelled(true); // 기본적으로 파괴 방지

        if (block.getType() == Material.DRAGON_EGG) {
            supplyDropManager.startEggBreak(event.getPlayer());
        } else {
            event.getPlayer().sendMessage("§c이 제단은 파괴할 수 없습니다.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        if (block.getType() == Material.DRAGON_EGG && supplyDropManager.isAltarBlock(block.getLocation())) {
            event.setCancelled(true); // 알 우클릭(텔레포트) 방지
            event.getPlayer().sendMessage("§c알을 우클릭할 수 없습니다. 파괴하여 보상을 획득하세요.");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().distanceSquared(event.getTo()) > 0.01) {
            supplyDropManager.cancelEggBreak(event.getPlayer());
        }
    }
}