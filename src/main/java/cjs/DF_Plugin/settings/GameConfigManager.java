package cjs.DF_Plugin.settings;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public class GameConfigManager {

    private final DF_Main plugin;
    private final FileConfiguration config;

    public GameConfigManager(DF_Main plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig(); // resources 폴더의 config.yml을 플러그인 폴더로 복사
        this.config = plugin.getConfig();
        loadDefaults();
    }

    private void loadDefaults() {
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void set(String path, Object value) {
        config.set(path, value);
    }

    public void save() {
        plugin.saveConfig();
    }

    /**
     * 모든 설정을 config.yml의 기본값으로 초기화합니다.
     */
    public void resetToDefaults() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (configFile.exists()) {
            if (!configFile.delete()) {
                plugin.getLogger().warning("config.yml 파일을 삭제할 수 없습니다. 수동으로 삭제 후 재시작해주세요.");
                return;
            }
        }
        plugin.saveDefaultConfig();
        plugin.reloadConfig(); // 새 기본 설정 파일을 메모리로 다시 로드
    }

    // 각 시스템의 활성화 여부를 쉽게 확인할 수 있는 헬퍼 메소드
    public boolean isPylonSystemEnabled() {
        return config.getBoolean("system-toggles.pylon", true);
    }

    public boolean isUpgradeSystemEnabled() {
        return config.getBoolean("system-toggles.upgrade", true);
    }

    public boolean isEventSystemEnabled() {
        return config.getBoolean("system-toggles.events", true);
    }
}