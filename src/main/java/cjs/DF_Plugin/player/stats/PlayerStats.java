package cjs.DF_Plugin.player.stats;

import java.util.HashMap;
import java.util.Map;

public class PlayerStats implements Cloneable {
    private Map<StatType, Integer> stats = new HashMap<>();

    public PlayerStats() {
        // 모든 스탯을 기본값 1로 초기화합니다.
        for (StatType type : StatType.values()) {
            stats.put(type, 1);
        }
    }

    public int getStat(StatType type) {
        return stats.getOrDefault(type, 1);
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

    @Override
    public PlayerStats clone() {
        try {
            PlayerStats cloned = (PlayerStats) super.clone();
            // The map field is mutable, so we need to create a new map for the clone.
            cloned.stats = new HashMap<>(this.stats);
            return cloned;
        } catch (CloneNotSupportedException e) {
            // This should not happen, as we are implementing Cloneable
            throw new AssertionError();
        }
    }
}