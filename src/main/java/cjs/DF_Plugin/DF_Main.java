package cjs.DF_Plugin;

import cjs.DF_Plugin.actionbar.ActionBarManager;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.clan.ClanManager;
import cjs.DF_Plugin.command.DFCommand;
import cjs.DF_Plugin.command.ItemNameCommand;
import cjs.DF_Plugin.command.DFTabCompleter;
import cjs.DF_Plugin.command.PylonStorageCommand;
import cjs.DF_Plugin.enchant.EnchantManager;
import cjs.DF_Plugin.events.game.GameStartManager;
import cjs.DF_Plugin.events.supplydrop.SupplyDropManager;
import cjs.DF_Plugin.enchant.EnchantListener;
import cjs.DF_Plugin.items.ItemNameManager;
import cjs.DF_Plugin.items.RecipeManager;
import cjs.DF_Plugin.items.SpecialItemListener;
import cjs.DF_Plugin.listener.*;
import cjs.DF_Plugin.offline.OfflinePlayerManager;
import cjs.DF_Plugin.player.PlayerRegistryManager;
import cjs.DF_Plugin.player.death.PlayerDeathManager;
import cjs.DF_Plugin.player.stats.StatsListener;
import cjs.DF_Plugin.player.stats.StatsManager;
import cjs.DF_Plugin.pylon.PylonManager;
import cjs.DF_Plugin.pylon.beaconinteraction.*;
import cjs.DF_Plugin.pylon.beacongui.recon.ReconManager;
import cjs.DF_Plugin.pylon.beacongui.giftbox.GiftBoxRefillTask;
import cjs.DF_Plugin.pylon.beacongui.BeaconGUIListener;
import cjs.DF_Plugin.settings.GameConfigManager;
import cjs.DF_Plugin.settings.GameModeManager;
import cjs.DF_Plugin.upgrade.UpgradeListener;
import cjs.DF_Plugin.upgrade.UpgradeManager;
import cjs.DF_Plugin.upgrade.specialability.CooldownStorage;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityListener;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import cjs.DF_Plugin.world.WorldManager;
import cjs.DF_Plugin.world.WorldLoadListener;
import cjs.DF_Plugin.events.end.EndEventManager;
import cjs.DF_Plugin.events.end.EndEventListener;
import cjs.DF_Plugin.events.supplydrop.AltarInteractionListener;
import cjs.DF_Plugin.world.nether.NetherManager;
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
    private SpecialAbilityManager specialAbilityManager;
    private ActionBarManager actionBarManager;
    private CooldownStorage cooldownStorage;
    private GameConfigManager gameConfigManager;
    private RecipeManager recipeManager;
    private GameModeManager gameModeManager;
    private EnchantManager enchantManager;
    private EndEventManager endEventManager;
    private NetherManager netherManager;
    private GameStartManager gameStartManager;
    private SupplyDropManager supplyDropManager;
    private ItemNameManager itemNameManager;

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
        this.enchantManager = new EnchantManager(this);
        this.endEventManager = new EndEventManager(this);
        this.netherManager = new NetherManager(this);
        this.gameStartManager = new GameStartManager(this);
        this.supplyDropManager = new SupplyDropManager(this);
        this.itemNameManager = new ItemNameManager(this);

        // 강화 시스템 매니저 초기화
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
        getCommand("df").setTabCompleter(new DFTabCompleter(this));
        getCommand("itemname").setExecutor(new ItemNameCommand(this));
        getCommand("pylonstorage").setExecutor(new PylonStorageCommand(this));

        // 이벤트 리스너 등록
        // 핵심 리스너
        getServer().getPluginManager().registerEvents(new UpgradeListener(this), this);
        getServer().getPluginManager().registerEvents(new SpecialAbilityListener(this), this);
        getServer().getPluginManager().registerEvents(new BeaconInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new PylonItemListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new BeaconGUIListener(this, this.pylonManager.getGuiManager()), this);
        getServer().getPluginManager().registerEvents(new StatsListener(this), this);
        getServer().getPluginManager().registerEvents(new SpecialItemListener(this), this);

        // 매니저 및 모듈 리스너
        getServer().getPluginManager().registerEvents(this.playerRegistryManager, this);
        getServer().getPluginManager().registerEvents(this.playerDeathManager, this);
        getServer().getPluginManager().registerEvents(this.pylonManager.getReconManager(), this);

        // 게임 규칙 및 월드 리스너
        getServer().getPluginManager().registerEvents(new GameRuleListener(this), this);
        getServer().getPluginManager().registerEvents(new BossMobListener(this), this);
        getServer().getPluginManager().registerEvents(new EnchantmentRuleListener(this), this);
        getServer().getPluginManager().registerEvents(new PylonProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new EndEventListener(this), this);
        getServer().getPluginManager().registerEvents(new EnchantListener(this), this);
        getServer().getPluginManager().registerEvents(new ClanNetherListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new DayNightListener(this), this);
        getServer().getPluginManager().registerEvents(new AltarInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldLoadListener(this), this);
        
        // 선물상자 자동 리필 작업 시작 (1분마다 확인)
        new GiftBoxRefillTask(this).runTaskTimer(this, 20L * 60, 20L * 60);

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
        if (this.clanManager != null) {
            clanManager.getPylonStorages().forEach((clanName, inventory) -> {
                Clan clan = clanManager.getClanByName(clanName);
                if (clan != null) clanManager.getStorageManager().savePylonStorage(clan, inventory);
            });
            clanManager.getGiftBoxInventories().forEach((clanName, inventory) -> {
                Clan clan = clanManager.getClanByName(clanName);
                if (clan != null) clanManager.getStorageManager().saveGiftBox(clan, inventory);
            });
        }
        if (this.playerDeathManager != null) {
            playerDeathManager.saveOnDisable();
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
    public SpecialAbilityManager getSpecialAbilityManager() { return specialAbilityManager; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }

    // 신규 설정 시스템 Getter
    public GameConfigManager getGameConfigManager() { return gameConfigManager; }
    public GameModeManager getGameModeManager() { return gameModeManager; }
    public RecipeManager getRecipeManager() { return recipeManager; }
    public EnchantManager getEnchantManager() { return enchantManager; }
    public EndEventManager getEndEventManager() { return endEventManager; }
    public ItemNameManager getItemNameManager() { return itemNameManager; }
    public GameStartManager getGameStartManager() { return gameStartManager; }
    public SupplyDropManager getSupplyDropManager() { return supplyDropManager; }

    public ReconManager getReconManager() {
        return this.pylonManager.getReconManager();
    }

    public static DF_Main getInstance() {
        return instance;
    }
}