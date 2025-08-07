package cjs.DF_Plugin.player.death;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 플레이어의 사망 및 밴 상태를 관리하는 클래스
 */
public class PlayerDeathManager implements Listener {
    private final DF_Main plugin;
    private File deathsFile;
    private FileConfiguration deathsConfig;
    private final Map<UUID, Long> deadPlayers = new ConcurrentHashMap<>(); // Player UUID -> Death Timestamp

    public PlayerDeathManager(DF_Main plugin) {
        this.plugin = plugin;
        setupDeathsFile();
        loadDeaths();
    }

    private void setupDeathsFile() {
        deathsFile = new File(plugin.getDataFolder(), "deaths.yml");
        if (!deathsFile.exists()) {
            plugin.saveResource("deaths.yml", false);
        }
        deathsConfig = YamlConfiguration.loadConfiguration(deathsFile);
    }

    private void loadDeaths() {
        if (deathsConfig.isConfigurationSection("deaths")) {
            for (String uuidString : deathsConfig.getConfigurationSection("deaths").getKeys(false)) {
                deadPlayers.put(UUID.fromString(uuidString), deathsConfig.getLong("deaths." + uuidString));
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        boolean deathBanEnabled = plugin.getPylonManager().getConfigManager().isDeathBanEnabled();
        if (!deathBanEnabled) return;

        Player player = event.getEntity();
        long deathTime = System.currentTimeMillis();
        deadPlayers.put(player.getUniqueId(), deathTime);
        deathsConfig.set("deaths." + player.getUniqueId().toString(), deathTime);
        saveDeathsFile();

        int banDurationHours = plugin.getPylonManager().getConfigManager().getDeathBanDurationHours();
        player.kickPlayer("§c사망하여 " + banDurationHours + "시간 동안 추방되었습니다.\n§e가문원에게 부활을 요청할 수 있습니다.");
    }

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        UUID playerUUID = event.getUniqueId();
        if (!deadPlayers.containsKey(playerUUID)) return;

        long deathTime = deadPlayers.get(playerUUID);
        int banDurationHours = plugin.getPylonManager().getConfigManager().getDeathBanDurationHours();
        long banEndTime = deathTime + TimeUnit.HOURS.toMillis(banDurationHours);

        if (System.currentTimeMillis() < banEndTime) {
            long remainingMillis = banEndTime - System.currentTimeMillis();
            String remainingTime = String.format("%02d시간 %02d분 %02d초",
                    TimeUnit.MILLISECONDS.toHours(remainingMillis),
                    TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
            );
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "§c사망으로 인해 추방되었습니다.\n§e남은 시간: " + remainingTime);
        } else {
            // Ban time is over, allow login and remove from list
            resurrectPlayer(playerUUID);
        }
    }

    public void resurrectPlayer(UUID playerUUID) {
        deadPlayers.remove(playerUUID);
        deathsConfig.set("deaths." + playerUUID.toString(), null);
        saveDeathsFile();
    }

    public Map<UUID, Long> getDeadPlayers() {
        return deadPlayers;
    }

    private void saveDeathsFile() {
        try {
            deathsConfig.save(deathsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save deaths.yml!");
            e.printStackTrace();
        }
    }
}