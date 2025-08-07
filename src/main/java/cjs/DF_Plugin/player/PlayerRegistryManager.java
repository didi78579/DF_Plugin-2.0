package cjs.DF_Plugin.player;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 서버에 접속한 모든 플레이어를 기록하고 관리하는 클래스
 */
public class PlayerRegistryManager implements Listener {
    private final DF_Main plugin;
    private File playersFile;
    private FileConfiguration playersConfig;
    private final Map<UUID, RegisteredPlayerData> allPlayers = new HashMap<>();

    public PlayerRegistryManager(DF_Main plugin) {
        this.plugin = plugin;
        setupPlayersFile();
        loadPlayers();
    }

    private void setupPlayersFile() {
        playersFile = new File(plugin.getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            plugin.saveResource("players.yml", false);
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    private void loadPlayers() {
        if (playersConfig.isConfigurationSection("players")) {
            ConfigurationSection playersSection = playersConfig.getConfigurationSection("players");
            if (playersSection == null) return;
            for (String uuidString : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String name = playersConfig.getString("players." + uuidString + ".name");
                    String clanName = playersConfig.getString("players." + uuidString + ".clan"); // can be null
                    if (name != null) {
                        allPlayers.put(uuid, new RegisteredPlayerData(name, clanName));
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in players.yml: " + uuidString);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updatePlayerClan(player.getUniqueId(), plugin.getClanManager().getClanByPlayer(player.getUniqueId()));
    }

    /**
     * 플레이어의 클랜 정보를 players.yml에 업데이트합니다.
     * @param playerUUID 업데이트할 플레이어의 UUID
     * @param clan 소속된 클랜 (없으면 null)
     */
    public void updatePlayerClan(UUID playerUUID, Clan clan) {
        RegisteredPlayerData currentData = allPlayers.get(playerUUID);
        String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
        if (playerName == null && currentData != null) {
            playerName = currentData.getName();
        }
        if (playerName == null) return; // Cannot find player name.

        String clanName = (clan != null) ? clan.getName() : null;

        // 데이터가 변경되었을 경우에만 업데이트
        if (currentData == null || !currentData.getName().equals(playerName) || !Objects.equals(currentData.getClanName(), clanName)) {
            allPlayers.put(playerUUID, new RegisteredPlayerData(playerName, clanName));
            playersConfig.set("players." + playerUUID + ".name", playerName);
            playersConfig.set("players." + playerUUID + ".clan", clanName);
            savePlayersFile();
        }
    }

    /**
     * 모집 가능한 플레이어(클랜에 소속되지 않은) 목록을 가져옵니다.
     * @return 모집 가능한 플레이어 UUID 목록
     */
    public List<UUID> getRecruitablePlayerUUIDs() {
        return allPlayers.entrySet().stream()
                .filter(entry -> entry.getValue().getClanName() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<UUID> getAllPlayerUUIDs() {
        return new ArrayList<>(allPlayers.keySet());
    }

    private void savePlayersFile() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save players.yml!");
            e.printStackTrace();
        }
    }

    /**
     * 플레이어 등록 정보를 담는 내부 클래스
     */
    private static class RegisteredPlayerData {
        private final String name;
        private final String clanName; // null if not in a clan

        public RegisteredPlayerData(String name, String clanName) {
            this.name = name;
            this.clanName = clanName;
        }

        public String getName() { return name; }

        public String getClanName() { return clanName; }
    }
}