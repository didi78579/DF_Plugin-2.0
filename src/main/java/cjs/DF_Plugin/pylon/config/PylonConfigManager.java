package cjs.DF_Plugin.pylon.config;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class PylonConfigManager {
    private final DF_Main plugin;
    private FileConfiguration config;

    private int maxPylons;
    private boolean requireBelowSeaLevel;
    private int areaEffectRadius;
    private boolean enemyDebuffsEnabled;
    private boolean reconEnabled;
    private int reconCooldownHours;
    private int reconReturnMinutes;
    private int giftboxCooldownHours;
    private int giftboxMinSets;
    private int giftboxMaxSets;
    private boolean recruitRandomDraw;
    private int recruitCostPerMember;
    private int clanMaxMembers;
    private boolean deathBanEnabled;
    private int deathBanDurationHours;
    private int resurrectionCostPerMinute;
    private int retrievalCooldownHours;
    private int reinstallDurationHours;

    public PylonConfigManager(DF_Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "pylon.yml");
        if (!configFile.exists()) {
            plugin.saveResource("pylon.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        this.maxPylons = config.getInt("clan.max-pylons", 1);
        this.requireBelowSeaLevel = config.getBoolean("installation.require-below-sea-level", true);
        this.areaEffectRadius = config.getInt("area-effects.radius", 50);
        this.enemyDebuffsEnabled = config.getBoolean("area-effects.enemy-debuffs.enabled", true);
        this.reconEnabled = config.getBoolean("recon-firework.enabled", true);
        this.reconCooldownHours = config.getInt("recon-firework.cooldown-hours", 12);
        this.reconReturnMinutes = config.getInt("recon-firework.return-duration-minutes", 1);
        this.giftboxCooldownHours = config.getInt("giftbox.cooldown-hours", 4);
        this.giftboxMinSets = config.getInt("giftbox.min-reward-sets", 4);
        this.giftboxMaxSets = config.getInt("giftbox.max-reward-sets", 8);
        this.recruitRandomDraw = config.getBoolean("recruitment.enable-random-draw", true);
        this.recruitCostPerMember = config.getInt("recruitment.cost-per-member", 64);
        this.clanMaxMembers = config.getInt("clan.max-members", 10);
        this.deathBanEnabled = config.getBoolean("death-ban.enabled", true);
        this.deathBanDurationHours = config.getInt("death-ban.duration-hours", 1);
        this.resurrectionCostPerMinute = config.getInt("death-ban.resurrection-cost-per-minute", 1);
        this.retrievalCooldownHours = config.getInt("retrieval.cooldown-hours", 24);
        this.reinstallDurationHours = config.getInt("retrieval.reinstall-duration-hours", 2);
    }

    public int getMaxPylons() { return maxPylons; }
    public boolean isRequireBelowSeaLevel() { return requireBelowSeaLevel; }
    public int getAreaEffectRadius() { return areaEffectRadius; }
    public boolean areEnemyDebuffsEnabled() { return enemyDebuffsEnabled; }
    public boolean isReconEnabled() { return reconEnabled; }
    public int getReconCooldownHours() { return reconCooldownHours; }
    public int getReconReturnMinutes() { return reconReturnMinutes; }
    public int getGiftboxCooldownHours() { return giftboxCooldownHours; }
    public int getGiftboxMinSets() { return giftboxMinSets; }
    public int getGiftboxMaxSets() { return giftboxMaxSets; }
    public boolean isRecruitRandomDraw() { return recruitRandomDraw; }
    public int getRecruitCostPerMember() { return recruitCostPerMember; }
    public int getClanMaxMembers() { return clanMaxMembers; }
    public boolean isDeathBanEnabled() { return deathBanEnabled; }
    public int getDeathBanDurationHours() { return deathBanDurationHours; }
    public int getResurrectionCostPerMinute() { return resurrectionCostPerMinute; }
    public int getRetrievalCooldownHours() { return retrievalCooldownHours; }
    public int getReinstallDurationHours() { return reinstallDurationHours; }
}