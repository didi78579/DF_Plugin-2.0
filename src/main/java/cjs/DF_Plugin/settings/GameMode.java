package cjs.DF_Plugin.settings;

import java.util.Arrays;

/**
 * 게임 모드의 종류와 각 모드별 시스템 활성화 상태를 정의하는 열거형 클래스입니다.
 */
public enum GameMode {
    DARKFOREST("darkforest", true, true, true),
    PYLON("pylon", true, false, false),
    UPGRADE("upgrade", false, true, false);

    private final String key;
    private final boolean pylonEnabled;
    private final boolean upgradeEnabled;
    private final boolean eventsEnabled;

    GameMode(String key, boolean pylonEnabled, boolean upgradeEnabled, boolean eventsEnabled) {
        this.key = key;
        this.pylonEnabled = pylonEnabled;
        this.upgradeEnabled = upgradeEnabled;
        this.eventsEnabled = eventsEnabled;
    }

    public String getKey() {
        return key;
    }

    public boolean isPylonEnabled() {
        return pylonEnabled;
    }

    public boolean isUpgradeEnabled() {
        return upgradeEnabled;
    }

    public boolean isEventsEnabled() {
        return eventsEnabled;
    }

    /**
     * 문자열 키로부터 해당하는 GameMode를 찾습니다.
     * @param key 찾을 게임 모드의 문자열 키 (예: "darkforest")
     * @return 해당하는 GameMode, 없으면 null
     */
    public static GameMode fromString(String key) {
        return Arrays.stream(values())
                .filter(mode -> mode.getKey().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }
}