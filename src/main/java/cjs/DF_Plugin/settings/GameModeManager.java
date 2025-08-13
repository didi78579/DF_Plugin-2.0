package cjs.DF_Plugin.settings;

import cjs.DF_Plugin.DF_Main;

public class GameModeManager {

    private final DF_Main plugin;
    private final GameConfigManager configManager;

    public GameModeManager(DF_Main plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getGameConfigManager();
    }

    /**
     * 지정된 게임 모드 프리셋을 적용합니다.
     * @param mode "darkforest", "pylon", "upgrade"
     */
    public void applyPreset(String mode) {
        GameMode gameMode = GameMode.fromString(mode);
        if (gameMode == null) {
            plugin.getLogger().warning("'" + mode + "'는 유효하지 않은 게임 모드입니다.");
            return; // 유효하지 않은 모드는 무시
        }

        applyPreset(gameMode);
    }

    public void applyPreset(GameMode gameMode) {
        configManager.getConfig().set("game-mode", gameMode.getKey());
        configManager.getConfig().set("system-toggles.pylon", gameMode.isPylonEnabled());
        configManager.getConfig().set("system-toggles.upgrade", gameMode.isUpgradeEnabled());
        configManager.getConfig().set("system-toggles.events", gameMode.isEventsEnabled());
        // 변경된 모든 설정을 파일에 한 번에 저장
        configManager.save();
    }
}