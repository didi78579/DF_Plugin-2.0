package cjs.DF_Plugin.upgrade.specialability;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 플레이어의 쿨다운 및 충전 정보를 파일에 저장하고 불러오는 클래스입니다.
 */
public class CooldownStorage {

    private final File dataFile;
    private FileConfiguration dataConfig;

    public CooldownStorage(DF_Main plugin) {
        this.dataFile = new File(plugin.getDataFolder(), "player_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void save(Map<UUID, Map<String, SpecialAbilityManager.CooldownInfo>> cooldowns, Map<UUID, Map<String, SpecialAbilityManager.ChargeInfo>> charges) {
        // 이전 데이터를 지우고 새로 씁니다.
        dataConfig.set("players", null);

        cooldowns.forEach((uuid, playerCooldowns) -> playerCooldowns.forEach((key, info) -> {
            String path = "players." + uuid.toString() + ".cooldowns." + key;
            dataConfig.set(path + ".endTime", info.endTime());
            dataConfig.set(path + ".displayName", info.displayName());
        }));

        charges.forEach((uuid, playerCharges) -> playerCharges.forEach((key, info) -> {
            String path = "players." + uuid.toString() + ".charges." + key;
            dataConfig.set(path + ".current", info.current());
            dataConfig.set(path + ".max", info.max());
            dataConfig.set(path + ".displayName", info.displayName());
        }));

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LoadedData load() {
        Map<UUID, Map<String, SpecialAbilityManager.CooldownInfo>> cooldowns = new HashMap<>();
        Map<UUID, Map<String, SpecialAbilityManager.ChargeInfo>> charges = new HashMap<>();
        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");

        if (playersSection == null) {
            return new LoadedData(cooldowns, charges);
        }

        long currentTime = System.currentTimeMillis();
        for (String uuidString : playersSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);

            // 쿨다운 정보 불러오기 (만료된 것은 제외)
            ConfigurationSection cooldownsSection = playersSection.getConfigurationSection(uuidString + ".cooldowns");
            if (cooldownsSection != null) {
                Map<String, SpecialAbilityManager.CooldownInfo> playerCooldowns = new HashMap<>();
                for (String key : cooldownsSection.getKeys(false)) {
                    long endTime = cooldownsSection.getLong(key + ".endTime");
                    if (endTime > currentTime) {
                        playerCooldowns.put(key, new SpecialAbilityManager.CooldownInfo(endTime, cooldownsSection.getString(key + ".displayName")));
                    }
                }
                if (!playerCooldowns.isEmpty()) cooldowns.put(uuid, playerCooldowns);
            }

            // 충전 정보 불러오기
            ConfigurationSection chargesSection = playersSection.getConfigurationSection(uuidString + ".charges");
            if (chargesSection != null) {
                Map<String, SpecialAbilityManager.ChargeInfo> playerCharges = new HashMap<>();
                for (String key : chargesSection.getKeys(false)) {
                    playerCharges.put(key, new SpecialAbilityManager.ChargeInfo(chargesSection.getInt(key + ".current"), chargesSection.getInt(key + ".max"), chargesSection.getString(key + ".displayName")));
                }
                if (!playerCharges.isEmpty()) charges.put(uuid, playerCharges);
            }
        }
        return new LoadedData(cooldowns, charges);
    }

    public record LoadedData(Map<UUID, Map<String, SpecialAbilityManager.CooldownInfo>> cooldowns, Map<UUID, Map<String, SpecialAbilityManager.ChargeInfo>> charges) {}
}