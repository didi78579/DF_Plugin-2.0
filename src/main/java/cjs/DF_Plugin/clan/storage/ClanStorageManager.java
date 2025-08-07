package cjs.DF_Plugin.clan.storage;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ClanStorageManager {
    private final DF_Main plugin;
    private File clanFile;
    private FileConfiguration clanConfig;
    private final String CLAN_SECTION = "clans";

    public ClanStorageManager(DF_Main plugin) {
        this.plugin = plugin;
        setupClanFile();
    }

    private void setupClanFile() {
        clanFile = new File(plugin.getDataFolder(), "clans.yml");
        if (!clanFile.exists()) {
            plugin.saveResource("clans.yml", false);
        }
        clanConfig = YamlConfiguration.loadConfiguration(clanFile);
        if (!clanConfig.isConfigurationSection(CLAN_SECTION)) {
            clanConfig.createSection(CLAN_SECTION);
            saveFile();
        }
    }

    public Map<String, Clan> loadAllClans() {
        Map<String, Clan> clans = new HashMap<>();
        ConfigurationSection clanSection = clanConfig.getConfigurationSection(CLAN_SECTION);
        if (clanSection == null) return clans;

        for (String clanName : clanSection.getKeys(false)) {
            ConfigurationSection section = clanSection.getConfigurationSection(clanName);
            if (section == null) continue;

            Clan clan = new Clan(clanName, section); // Clan 생성자를 통해 모든 데이터 로드

            clans.put(clanName.toLowerCase(), clan);
        }
        return clans;
    }

    public void saveClan(Clan clan) {
        String path = CLAN_SECTION + "." + clan.getName();
        clan.save(clanConfig.createSection(path)); // Clan의 save 메소드 활용
        saveFile();
    }

    public void deleteClan(String clanName) {
        clanConfig.set(CLAN_SECTION + "." + clanName, null);
        saveFile();
    }

    private void saveFile() {
        try {
            clanConfig.save(clanFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save clans.yml!", e);
        }
    }
}