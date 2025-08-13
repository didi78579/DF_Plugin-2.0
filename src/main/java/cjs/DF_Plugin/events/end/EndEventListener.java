package cjs.DF_Plugin.events.end;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EndEventListener implements Listener {

    private final EndEventManager endEventManager;

    public EndEventListener(DF_Main plugin) {
        this.endEventManager = plugin.getEndEventManager();
    }

    @EventHandler
    public void onPlayerEnterEndPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            return;
        }

        if (!endEventManager.isEndOpen()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c엔드 포탈은 아직 굳게 닫혀 있습니다.");
            return;
        }

        // 기본 이동을 취소하고 수동으로 텔레포트
        event.setCancelled(true);
        Player player = event.getPlayer();
        World endWorld = Bukkit.getWorld("world_the_end");
        if (endWorld != null) {
            double x = (Math.random() * 100) - 50;
            double z = (Math.random() * 100) - 50;
            double y = 250;
            Location randomLocation = new Location(endWorld, x, y, z);

            player.teleport(randomLocation);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 30 * 20, 0, true, false));
        }
    }

    @EventHandler
    public void onPlayerExitEnd(PlayerTeleportEvent event) {
        // 엔드에서 오버월드로 메인 포탈을 통해 나갈 때
        if (event.getFrom().getWorld().getEnvironment() == World.Environment.THE_END &&
                event.getTo().getWorld().getEnvironment() == World.Environment.NORMAL &&
                event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {

            event.setCancelled(true);
            endEventManager.teleportPlayerToSafety(event.getPlayer());
        }
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            // 보상 및 붕괴 시퀀스 시작
            endEventManager.triggerDragonDefeatSequence();
        }
    }
}