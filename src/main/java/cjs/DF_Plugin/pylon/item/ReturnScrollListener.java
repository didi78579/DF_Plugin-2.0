package cjs.DF_Plugin.pylon.item;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class ReturnScrollListener implements Listener {

    private final ReturnScrollManager scrollManager;

    public ReturnScrollListener(ReturnScrollManager scrollManager) {
        this.scrollManager = scrollManager;
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHandItem = event.getMainHandItem();

        if (PylonItemFactory.isReturnScroll(mainHandItem)) {
            event.setCancelled(true);
            scrollManager.startCasting(player);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player) {
            if (scrollManager.isCasting(victim)) {
                scrollManager.cancelCasting(victim, true);
            }
        }
    }
}