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
        configManager.set("game-mode", mode);

        switch (mode.toLowerCase()) {
            case "darkforest":
                applyDarkForestPreset();
                break;
            case "pylon":
                applyPylonPreset();
                break;
            case "upgrade":
                applyUpgradePreset();
                break;
            default:
                return; // 유효하지 않은 모드는 무시
        }
        // 변경된 모든 설정을 파일에 한 번에 저장
        configManager.save();
    }

    private void applyDarkForestPreset() {
        configManager.set("system-toggles.pylon", true);
        configManager.set("system-toggles.upgrade", true);
        configManager.set("system-toggles.events", true);
    }

    private void applyPylonPreset() {
        configManager.set("system-toggles.pylon", true);
        configManager.set("system-toggles.upgrade", false);
        configManager.set("system-toggles.events", false);
    }

    private void applyUpgradePreset() {
        configManager.set("system-toggles.pylon", false);
        configManager.set("system-toggles.upgrade", true);
        configManager.set("system-toggles.events", false);
    }
}