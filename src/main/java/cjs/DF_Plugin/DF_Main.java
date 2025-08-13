package cjs.DF_Plugin;

import cjs.DF_Plugin.actionbar.ActionBarManager;
import cjs.DF_Plugin.clan.ClanManager;
import cjs.DF_Plugin.command.DFCommand;
import cjs.DF_Plugin.listener.PlayerChatListener;
import cjs.DF_Plugin.listener.ClanNetherListener;
import cjs.DF_Plugin.listener.BossMobListener;
import cjs.DF_Plugin.listener.EnchantmentRuleListener;
import cjs.DF_Plugin.listener.GameRuleListener;
import cjs.DF_Plugin.listener.PlayerJoinListener;
import cjs.DF_Plugin.listener.PlayerMoveListener;
import cjs.DF_Plugin.misc.RecipeManager;
import cjs.DF_Plugin.offline.OfflinePlayerManager;
import cjs.DF_Plugin.player.PlayerRegistryManager;
import cjs.DF_Plugin.player.death.PlayerDeathManager;
import cjs.DF_Plugin.player.stats.StatsListener;
import cjs.DF_Plugin.player.stats.StatsManager;
import cjs.DF_Plugin.pylon.PylonManager;
import cjs.DF_Plugin.pylon.beaconinteraction.BeaconInteractionListener;
import cjs.DF_Plugin.pylon.beaconinteraction.PylonItemListener;
import cjs.DF_Plugin.pylon.beacongui.BeaconGUIListener;
import cjs.DF_Plugin.settings.GameConfigManager;
import cjs.DF_Plugin.settings.GameModeManager;
import cjs.DF_Plugin.upgrade.UpgradeListener;
import cjs.DF_Plugin.upgrade.UpgradeManager;
import cjs.DF_Plugin.pylon.beaconinteraction.PylonProtectionListener;
import cjs.DF_Plugin.upgrade.profile.WeaponProfileManager;
import cjs.DF_Plugin.upgrade.setting.UpgradeSettingManager;
import cjs.DF_Plugin.upgrade.specialability.CooldownStorage;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityListener;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import cjs.DF_Plugin.world.WorldManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class DF_Main extends JavaPlugin {

    private static DF_Main instance;

    private ClanManager clanManager;
    private PylonManager pylonManager;
    private UpgradeManager upgradeManager;
    private WorldManager worldManager;
    private PlayerRegistryManager playerRegistryManager;
    private PlayerDeathManager playerDeathManager;
    private StatsManager statsManager;
    private OfflinePlayerManager offlinePlayerManager;
    private UpgradeSettingManager upgradeSettingManager;
    private WeaponProfileManager weaponProfileManager;
    private SpecialAbilityManager specialAbilityManager;
    private ActionBarManager actionBarManager;
    private CooldownStorage cooldownStorage;
    private GameConfigManager gameConfigManager;
    private RecipeManager recipeManager;
    private GameModeManager gameModeManager;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("Initializing managers for DarkForest 2.0...");

        // 설정 및 데이터 저장소 초기화
        this.gameConfigManager = new GameConfigManager(this);
        this.cooldownStorage = new CooldownStorage(this);
        CooldownStorage.LoadedData loadedData = cooldownStorage.load();

        // 핵심 매니저 초기화
        this.gameModeManager = new GameModeManager(this);
        this.recipeManager = new RecipeManager(this);
        this.clanManager = new ClanManager(this);
        this.pylonManager = new PylonManager(this);
        this.worldManager = new WorldManager(this);
        this.playerRegistryManager = new PlayerRegistryManager(this);
        this.playerDeathManager = new PlayerDeathManager(this);
        this.statsManager = new StatsManager(this);
        this.offlinePlayerManager = new OfflinePlayerManager(this);

        // 강화 시스템 매니저 초기화
        this.upgradeSettingManager = new UpgradeSettingManager(this);
        this.weaponProfileManager = new WeaponProfileManager(this);
        this.specialAbilityManager = new SpecialAbilityManager(this, loadedData.cooldowns(), loadedData.charges());
        this.upgradeManager = new UpgradeManager(this);
        this.actionBarManager = new ActionBarManager(this, this.specialAbilityManager);

        // 설정 기반 기능 초기화
        this.recipeManager.updateRecipes();
        this.worldManager.applyAllWorldSettings();

        getLogger().info("Managers initialized successfully.");

        // 커맨드 등록
        DFCommand dfCommand = new DFCommand(this);
        getCommand("df").setExecutor(dfCommand);
        getCommand("df").setTabCompleter(dfCommand);

        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new UpgradeListener(this), this);
        getServer().getPluginManager().registerEvents(new SpecialAbilityListener(this), this);
        getServer().getPluginManager().registerEvents(new BeaconInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new PylonItemListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(this.clanManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this.clanManager), this);
        getServer().getPluginManager().registerEvents(new BeaconGUIListener(this, this.pylonManager.getGuiManager()), this);
        getServer().getPluginManager().registerEvents(this.playerRegistryManager, this);
        getServer().getPluginManager().registerEvents(this.playerDeathManager, this);
        getServer().getPluginManager().registerEvents(this.pylonManager.getReconManager(), this);
        getServer().getPluginManager().registerEvents(new StatsListener(this.statsManager), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new GameRuleListener(this), this);
        getServer().getPluginManager().registerEvents(new BossMobListener(this), this);
        getServer().getPluginManager().registerEvents(new EnchantmentRuleListener(this), this);
        getServer().getPluginManager().registerEvents(new PylonProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ClanNetherListener(this), this);

        getLogger().info("DarkForest 2.0 plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // 플레이어 데이터 저장
        if (this.statsManager != null) {
            this.statsManager.saveStats();
        }
        // 쿨다운 및 충전 정보 저장
        if (this.cooldownStorage != null && this.specialAbilityManager != null) {
            cooldownStorage.save(specialAbilityManager.getCooldownsMap(), specialAbilityManager.getChargesMap());
        }
        getLogger().info("DarkForest 2.0 plugin has been disabled.");
    }

    // 다른 클래스에서 매니저에 접근할 수 있도록 Getter를 제공합니다.
    public ClanManager getClanManager() { return clanManager; }
    public PylonManager getPylonManager() { return pylonManager; }
    public PlayerRegistryManager getPlayerRegistryManager() { return playerRegistryManager; }
    public PlayerDeathManager getPlayerDeathManager() { return playerDeathManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public WorldManager getWorldManager() { return worldManager; }

    // 강화 시스템 Getter
    public UpgradeManager getUpgradeManager() { return upgradeManager; }
    public UpgradeSettingManager getUpgradeSettingManager() { return upgradeSettingManager; }
    public WeaponProfileManager getWeaponProfileManager() { return weaponProfileManager; }
    public SpecialAbilityManager getSpecialAbilityManager() { return specialAbilityManager; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }

    // 신규 설정 시스템 Getter
    public GameConfigManager getGameConfigManager() { return gameConfigManager; }
    public GameModeManager getGameModeManager() { return gameModeManager; }
    public RecipeManager getRecipeManager() { return recipeManager; }

    public static DF_Main getInstance() {
        return instance;
    }
}