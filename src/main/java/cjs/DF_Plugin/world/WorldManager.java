package cjs.DF_Plugin.world;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.settings.GameConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;

public class WorldManager {

    private final DF_Main plugin;

    public WorldManager(DF_Main plugin) {
        this.plugin = plugin;
    }

    /**
     * config.yml에 정의된 모든 월드 관련 설정을 불러와 적용합니다.
     */
    public void applyAllWorldSettings() {
        applyGameRules();
        applyWorldBorders();
    }

    /**
     * 좌표 숨기기 등 게임 규칙을 적용합니다.
     */
    private void applyGameRules() {
        GameConfigManager configManager = plugin.getGameConfigManager();
        boolean locationInfoDisabled = configManager.getConfig().getBoolean("utility.location-info-disabled", true);

        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.REDUCED_DEBUG_INFO, locationInfoDisabled);
        }
    }

    /**
     * 월드 보더 크기를 적용합니다.
     */
    private void applyWorldBorders() {
        GameConfigManager configManager = plugin.getGameConfigManager();
        double overworldSize = configManager.getConfig().getDouble("worldborder.overworld-size", 20000);
        boolean endEnabled = configManager.getConfig().getBoolean("worldborder.end-enabled", true);

        for (World world : Bukkit.getWorlds()) {
            WorldBorder border = world.getWorldBorder();
            if (world.getEnvironment() == World.Environment.NORMAL) {
                border.setCenter(0, 0);
                border.setSize(overworldSize);
            } else if (world.getEnvironment() == World.Environment.THE_END) {
                border.setCenter(0, 0);
                border.setSize(endEnabled ? 1000 : 60000000); // 활성화 시 1000, 비활성화 시 최대
            }
        }
    }

    /**
     * 클랜의 고유한 네더 월드 이름을 반환합니다.
     * @param clan 대상 클랜
     * @return 클랜 네더 월드 이름 (e.g., clan_nether_MyClan)
     */
    public String getClanNetherWorldName(Clan clan) {
        return "clan_nether_" + clan.getName().toLowerCase();
    }

    /**
     * 지정된 클랜의 네더 월드를 가져오거나, 없으면 새로 생성합니다.
     * @param clan 대상 클랜
     * @return 클랜의 네더 월드
     */
    public World getOrCreateClanNether(Clan clan) {
        String worldName = getClanNetherWorldName(clan);
        return Bukkit.getWorld(worldName) != null ? Bukkit.getWorld(worldName) : new WorldCreator(worldName).environment(World.Environment.NETHER).createWorld();
    }
}