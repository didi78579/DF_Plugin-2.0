package cjs.DF_Plugin.pylon;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.pylon.beaconinteraction.PylonAreaManager;
import cjs.DF_Plugin.pylon.beacongui.BeaconGUIManager;
import cjs.DF_Plugin.pylon.config.PylonConfigManager;
import cjs.DF_Plugin.pylon.beaconinteraction.registration.BeaconRegistrationManager;
import cjs.DF_Plugin.pylon.beacongui.recon.ReconManager;
import cjs.DF_Plugin.pylon.reinstall.PylonReinstallManager;
import cjs.DF_Plugin.pylon.retrieval.PylonRetrievalManager;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.TimeUnit;

public class PylonManager {

    private final DF_Main plugin;
    private final BeaconRegistrationManager registrationManager;
    private final PylonAreaManager areaManager;
    private final PylonConfigManager configManager;
    private final BeaconGUIManager guiManager;
    private final PylonRetrievalManager retrievalManager;
    private final PylonReinstallManager reinstallManager;
    private final ReconManager reconManager;

    public PylonManager(DF_Main plugin) {
        this.plugin = plugin;
        this.configManager = new PylonConfigManager(plugin);
        this.registrationManager = new BeaconRegistrationManager(plugin);
        this.areaManager = new PylonAreaManager(plugin);
        this.guiManager = new BeaconGUIManager(plugin);
        this.retrievalManager = new PylonRetrievalManager(plugin);
        this.reinstallManager = new PylonReinstallManager(plugin);
        this.reconManager = new ReconManager(plugin);
        startGiftBoxTask();
        startAreaEffectTask();
        plugin.getLogger().info("PylonManager loaded.");
    }

    public BeaconRegistrationManager getRegistrationManager() {
        return registrationManager;
    }

    public PylonAreaManager getAreaManager() {
        return areaManager;
    }

    public PylonConfigManager getConfigManager() {
        return configManager;
    }

    public BeaconGUIManager getGuiManager() {
        return guiManager;
    }

    public PylonRetrievalManager getRetrievalManager() {
        return retrievalManager;
    }

    public PylonReinstallManager getReinstallManager() {
        return reinstallManager;
    }

    public ReconManager getReconManager() {
        return reconManager;
    }

    private void startGiftBoxTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long cooldownMillis = TimeUnit.HOURS.toMillis(configManager.getGiftboxCooldownHours());
                for (Clan clan : plugin.getClanManager().getClans()) {
                    if (!clan.isGiftBoxReady() && (System.currentTimeMillis() - clan.getLastGiftBoxTime() >= cooldownMillis)) {
                        clan.setGiftBoxReady(true);
                        plugin.getClanManager().getStorageManager().saveClan(clan);

                        // Notify the leader if they are online
                        Player leader = Bukkit.getPlayer(clan.getLeader());
                        if (leader != null && leader.isOnline()) {
                            leader.sendMessage(PluginUtils.colorize("&e[선물상자] &a파일런의 선물상자가 보상으로 가득 찼습니다!"));
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 60); // Run every minute
    }

    private void startAreaEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                areaManager.applyAreaEffects();
            }
        }.runTaskTimer(plugin, 100L, 100L); // Run every 5 seconds
    }
}