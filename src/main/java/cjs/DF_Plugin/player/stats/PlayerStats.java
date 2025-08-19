package cjs.DF_Plugin.player.stats;

import java.util.HashMap;
import java.util.Map;

public class PlayerStats implements Cloneable {
    private Map<StatType, Integer> stats = new HashMap<>();
    private int kills = 0;
    private int deaths = 0;

    public PlayerStats() {
        // 모든 스탯을 기본값 3으로 초기화합니다.
        for (StatType type : StatType.values()) {
            stats.put(type, 3);
        }
    }

    public int getStat(StatType type) {
        return stats.getOrDefault(type, 3);
    }

    public void setStat(StatType type, int value) {
        // 값의 범위를 1~5로 제한합니다.
        int clampedValue = Math.max(1, Math.min(5, value));
        stats.put(type, clampedValue);
    }

    public double getCombatPower() {
        // 예능감은 전투력 계산에서 제외하고, 각 스탯에 가중치를 부여합니다.
        double attack = getStat(StatType.ATTACK) * 1.5;
        double intelligence = getStat(StatType.INTELLIGENCE) * 1.0;
        double stamina = getStat(StatType.STAMINA) * 1.2;
        return attack + intelligence + stamina;
    }

    public Map<StatType, Integer> getAllStats() {
        return this.stats;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public void incrementKills() {
        this.kills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = Math.max(0, deaths);
    }

    public void incrementDeaths() {
        this.deaths++;
    }

    /**
     * 모든 스탯이 기본값(3)인지 확인합니다.
     * @return 모든 스탯이 3이면 true, 아니면 false
     */
    public boolean isDefault() {
        if (kills != 0 || deaths != 0) return false;
        for (int value : stats.values()) {
            if (value != 3) return false;
        }
        return true;
    }

    @Override
    public PlayerStats clone() {
        try {
            PlayerStats cloned = (PlayerStats) super.clone();
            // The map field is mutable, so we need to create a new map for the clone.
            cloned.stats = new HashMap<>(this.stats);
            cloned.kills = this.kills;
            cloned.deaths = this.deaths;
            return cloned;
        } catch (CloneNotSupportedException e) {
            // This should not happen, as we are implementing Cloneable
            throw new AssertionError();
        }
    }
}