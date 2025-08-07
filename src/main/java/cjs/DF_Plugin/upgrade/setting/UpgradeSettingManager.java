package cjs.DF_Plugin.upgrade.setting;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class UpgradeSettingManager {
    private final DF_Main plugin;
    private final File configFile;
    private final FileConfiguration config;

    public UpgradeSettingManager(DF_Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "upgrade.yml");
        if (!configFile.exists()) {
            plugin.saveResource("upgrade.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}