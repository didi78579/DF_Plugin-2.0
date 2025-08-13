package cjs.DF_Plugin.items;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.*;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.Optional;

public class SpecialItemListener implements Listener {

    private final DF_Main plugin;

    public SpecialItemListener(DF_Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // 워든 처치 시 흑요석 포션 드롭
        if (event.getEntity() instanceof Warden) {
            event.getDrops().add(ObsidianPotion.createObsidianPotion());
        }

        // 엔더 드래곤 처치 시 마스터 컴퍼스 지급
        if (event.getEntity() instanceof EnderDragon) {
            Player killer = event.getEntity().getKiller();
            if (killer != null) {
                killer.getInventory().addItem(MasterCompass.createMasterCompass());
                killer.sendMessage(PluginUtils.colorize("&5[알림] &f드래곤의 힘이 깃든 나침반을 획득했습니다."));
            }
        }
    }

    @EventHandler
    public void onCompassUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!MasterCompass.isMasterCompass(item)) {
            return;
        }

        event.setCancelled(true);

        Clan playerClan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (playerClan == null) {
            player.sendMessage(PluginUtils.colorize("&c가문에 소속되어 있지 않아 사용할 수 없습니다."));
            return;
        }

        // 가장 가까운 '적' 파일런 찾기
        Optional<Location> nearestPylon = plugin.getClanManager().getClans().stream()
                .filter(clan -> !clan.equals(playerClan)) // 다른 가문 필터링
                .flatMap(clan -> clan.getPylonLocations().stream()) // 모든 파일런 위치 스트림
                .map(PluginUtils::deserializeLocation) // Location 객체로 변환
                .filter(loc -> loc != null && loc.getWorld().equals(player.getWorld())) // 같은 월드에 있는 파일런만
                .min(Comparator.comparingDouble(loc -> player.getLocation().distanceSquared(loc))); // 가장 가까운 위치 찾기

        if (nearestPylon.isPresent()) {
            Location target = nearestPylon.get();
            player.sendMessage(PluginUtils.colorize("&a[마스터 컴퍼스] &f가장 가까운 적의 파일런을 향해 기운을 발산합니다."));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
            spawnParticleTrail(player.getEyeLocation(), target);
        } else {
            player.sendMessage(PluginUtils.colorize("&c[마스터 컴퍼스] &f주변에서 다른 가문의 파일런을 찾을 수 없습니다."));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
        }
    }

    private void spawnParticleTrail(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.PURPLE, 2.0F);

        for (double i = 1; i < 5; i += 0.5) {
            Location particleLoc = start.clone().add(direction.clone().multiply(i));
            start.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, dustOptions);
        }
    }
}