package cjs.DF_Plugin.pylon.beaconinteraction;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.pylon.item.PylonItemFactory;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class BeaconInteractionListener implements Listener {

    private final DF_Main plugin;
    private static final String PREFIX = PluginUtils.colorize("&b[파일런] &f");

    public BeaconInteractionListener(DF_Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPylonPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItemInHand();

        if (block.getType() != Material.BEACON) {
            return;
        }

        boolean isMainCore = PylonItemFactory.isMainCore(itemInHand);
        boolean isAuxiliaryCore = PylonItemFactory.isAuxiliaryCore(itemInHand);

        if (!isMainCore && !isAuxiliaryCore) {
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(PREFIX + "§c가문에 소속되어 있어야 파일런을 설치할 수 있습니다.");
            event.setCancelled(true);
            return;
        }

        int maxPylons = plugin.getGameConfigManager().getPylonMaxPerClan();
        if (clan.getPylonLocations().size() >= maxPylons) {
            player.sendMessage(PREFIX + "§c가문이 가질 수 있는 최대 파일런 개수(" + maxPylons + "개)에 도달했습니다.");
            event.setCancelled(true);
            return;
        }

        if (isMainCore) {
            boolean requireBelowSeaLevel = plugin.getGameConfigManager().isPylonRequireBelowSeaLevel();
            if (requireBelowSeaLevel && block.getLocation().getBlockY() >= block.getWorld().getSeaLevel()) {
                player.sendMessage(PREFIX + "§c파일런 코어는 해수면 아래에만 설치할 수 있습니다.");
                event.setCancelled(true);
                return;
            }
            plugin.getPylonManager().getRegistrationManager().registerPylon(player, block, clan);

        } else if (isAuxiliaryCore) {
            // 보조 파일런은 기존 파일런 영역 내에만 설치 가능
            if (!plugin.getPylonManager().getAreaManager().isLocationInClanPylonArea(clan, block.getLocation())) {
                player.sendMessage(PREFIX + "§c보조 파일런은 기존 파일런의 영역 내에만 설치할 수 있습니다.");
                event.setCancelled(true);
                return;
            }
            plugin.getPylonManager().getAuxiliaryRegistrationManager().registerAuxiliaryPylon(player, block, clan);
        }
    }
}