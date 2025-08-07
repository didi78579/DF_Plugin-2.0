package cjs.DF_Plugin.player.stats;

public enum StatType {
    ATTACK("공격력"),
    INTELLIGENCE("지능"),
    STAMINA("체력"),
    ENTERTAINMENT("예능감");

    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}