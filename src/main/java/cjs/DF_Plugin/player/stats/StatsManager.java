package cjs.DF_Plugin.player.stats;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class StatsManager {

    private final DF_Main plugin;
    private final Map<UUID, MassRegistrationSession> massRegistrationSessions = new HashMap<>();
    private final Map<UUID, PlayerStats> statsCache = new ConcurrentHashMap<>();
    private final File statsFile;
    private FileConfiguration statsConfig;

    public StatsManager(DF_Main plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "player_stats.yml");
        loadStats();
    }

    public void loadStats() {
        if (!statsFile.exists()) {
            plugin.saveResource("player_stats.yml", false);
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        statsCache.clear();

        ConfigurationSection playersSection = statsConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidStr : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    PlayerStats stats = new PlayerStats();
                    for (StatType type : StatType.values()) {
                        int value = statsConfig.getInt("players." + uuidStr + "." + type.name(), 1);
                        stats.setStat(type, value);
                    }
                    statsCache.put(uuid, stats);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in player_stats.yml: " + uuidStr);
                }
            }
        }
        plugin.getLogger().info(statsCache.size() + " player stats loaded.");
    }

    public void saveStats() {
        for (Map.Entry<UUID, PlayerStats> entry : statsCache.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerStats stats = entry.getValue();
            for (Map.Entry<StatType, Integer> statEntry : stats.getAllStats().entrySet()) {
                statsConfig.set("players." + uuid.toString() + "." + statEntry.getKey().name(), statEntry.getValue());
            }
        }
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player stats to file!", e);
        }
    }

    public PlayerStats getPlayerStats(UUID playerUUID) {
        return statsCache.computeIfAbsent(playerUUID, k -> new PlayerStats());
    }

    public void openEditor(Player editor, Player target) {
        PlayerStats stats = getPlayerStats(target.getUniqueId());
        editor.openInventory(StatsEditor.create(target, stats));
    }

    /**
     * 인벤토리 기반 스탯 편집기(StatsListener)에서 사용하는 스탯 업데이트 메소드입니다.
     * @param target 스탯이 변경될 플레이어
     * @param type 변경할 스탯 종류
     * @param increment 증가(true) 또는 감소(false)
     */
    public void updateStatFromGUI(Player target, StatType type, boolean increment) {
        PlayerStats stats = getPlayerStats(target.getUniqueId());
        int currentValue = stats.getStat(type);
        stats.setStat(type, increment ? currentValue + 1 : currentValue - 1);
    }

    public void startMassRegistration(Player editor) {
        if (massRegistrationSessions.containsKey(editor.getUniqueId())) {
            editor.sendMessage("§c이미 스탯 평가를 진행 중입니다. 종료하려면 /df cancelstat 를 입력하세요.");
            return;
        }

        List<UUID> targets = plugin.getPlayerRegistryManager().getAllPlayerUUIDs();
        targets.remove(editor.getUniqueId()); // 자기 자신은 평가에서 제외

        if (targets.isEmpty()) {
            editor.sendMessage("§c평가할 다른 플레이어가 없습니다.");
            return;
        }

        MassRegistrationSession session = new MassRegistrationSession(editor.getUniqueId(), targets);
        UUID firstTarget = session.getNextTarget(); // 첫 번째 대상 가져오기
        if (firstTarget == null) {
            editor.sendMessage("§c평가할 다른 플레이어가 없습니다.");
            return;
        }
        session.setCurrentStats(getPlayerStats(firstTarget).clone()); // 첫 대상의 스탯으로 세션 초기화

        massRegistrationSessions.put(editor.getUniqueId(), session);
        editor.sendMessage("§a총 " + targets.size() + "명의 플레이어에 대한 스탯 평가를 시작합니다.");
        displayNextPlayerForRegistration(editor);
    }

    private void displayNextPlayerForRegistration(Player editor) {
        MassRegistrationSession session = massRegistrationSessions.get(editor.getUniqueId());
        if (session == null) return;

        UUID targetUUID = session.getCurrentTarget();
        if (targetUUID == null) {
            endMassRegistration(editor);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        StatsChatEditor.sendEditor(editor, target, session.getCurrentStats(), session.getProgress());
    }

    public void updateStatInSession(Player editor, StatType type, int value) {
        MassRegistrationSession session = massRegistrationSessions.get(editor.getUniqueId());
        if (session == null || session.getCurrentStats() == null) return;

        session.getCurrentStats().setStat(type, value);
        displayNextPlayerForRegistration(editor); // 변경사항을 반영하여 UI를 다시 표시
    }

    public void confirmAndNext(Player editor) {
        MassRegistrationSession session = massRegistrationSessions.get(editor.getUniqueId());
        if (session == null) return;

        // 현재 스탯을 실제 캐시에 저장
        UUID currentTargetUUID = session.getCurrentTarget();
        if (currentTargetUUID != null) {
            statsCache.put(currentTargetUUID, session.getCurrentStats());
            saveStats(); // 파일에 즉시 저장
            editor.sendMessage("§a" + Bukkit.getOfflinePlayer(currentTargetUUID).getName() + "님의 스탯을 저장했습니다.");
        }

        // 다음 플레이어로 이동
        if (session.hasNext()) {
            UUID nextTargetUUID = session.getNextTarget();
            session.setCurrentStats(getPlayerStats(nextTargetUUID).clone());
            displayNextPlayerForRegistration(editor);
        } else {
            editor.sendMessage("§a모든 플레이어의 스탯 평가를 완료했습니다!");
            endMassRegistration(editor);
        }
    }

    public void endMassRegistration(Player editor) {
        if (massRegistrationSessions.remove(editor.getUniqueId()) != null) {
            editor.sendMessage("§c스탯 평가를 종료했습니다.");
        }
    }
}