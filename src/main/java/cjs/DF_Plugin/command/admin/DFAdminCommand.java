package cjs.DF_Plugin.command.admin;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.command.admin.editor.SettingsEditor;
import cjs.DF_Plugin.enchant.MagicStone;
import cjs.DF_Plugin.player.stats.StatType;
import cjs.DF_Plugin.player.stats.StatsManager;
import cjs.DF_Plugin.settings.GameConfigManager;
import cjs.DF_Plugin.upgrade.profile.IWeaponProfile;
import cjs.DF_Plugin.settings.GameModeManager;
import cjs.DF_Plugin.upgrade.UpgradeManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class DFAdminCommand implements CommandExecutor {

    private final DF_Main plugin;
    private final GameModeManager gameModeManager;
    private final GameConfigManager configManager;
    private final SettingsEditor settingsEditor;
    private final SetSettingsCommand setCommand;
    private final StatsManager statsManager;

    public DFAdminCommand(DF_Main plugin) {
        this.plugin = plugin;
        this.gameModeManager = plugin.getGameModeManager();
        this.configManager = plugin.getGameConfigManager();
        this.settingsEditor = new SettingsEditor(plugin);
        this.setCommand = new SetSettingsCommand(plugin);
        this.statsManager = plugin.getStatsManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c사용법: /df admin <subcommand>");
            sender.sendMessage("§c사용 가능한 명령어: gamemode, settings, set, weapon, clan, register, controlender, unban, magicstone");

            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (subCommand) {
            case "gamemode" -> handleGameModeCommand(sender, subArgs);
            case "settings" -> handleSettingsCommand(sender, subArgs);
            case "weapon" -> handleWeaponCommand(sender, subArgs);
            case "clan" -> handleAdminClanCommand(sender, subArgs);
            case "set" -> setCommand.handle(sender, subArgs);
            case "register" -> handleRegisterCommand(sender, subArgs);
            case "setstat" -> handleSetStatCommand(sender, subArgs);
            case "confirmstat" -> handleConfirmStatCommand(sender);
            case "cancelstat" -> handleCancelStatCommand(sender);
            case "controlender" -> handleControlEnderCommand(sender, subArgs);
            case "unban" -> handleUnbanCommand(sender, subArgs);
            case "magicstone" -> handleMagicStoneCommand(sender, subArgs);
            default -> sender.sendMessage("§c알 수 없는 명령어입니다.");
        }

        return true;
    }

    private void handleGameModeCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("df.admin.gamemode")) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§c사용법: /df admin gamemode <darkforest|pylon|upgrade>");
            return;
        }

        String mode = args[0].toLowerCase();
        if (!Arrays.asList("darkforest", "pylon", "upgrade").contains(mode)) {
            sender.sendMessage("§c잘못된 게임 모드입니다. <darkforest|pylon|upgrade> 중에서 선택하세요.");
            return;
        }

        gameModeManager.applyPreset(mode);
        sender.sendMessage("§a게임 모드가 '" + mode + "'(으)로 설정되었습니다.");
        sender.sendMessage("§e서버를 재시작하거나 리로드해야 모든 설정이 적용될 수 있습니다.");
    }

    private void handleSettingsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("df.admin.settings")) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return;
        }

        if (args.length == 0) {
            settingsEditor.openMainMenu(player);
            return;
        }

        String category = args[0].toLowerCase();

        switch (category) {
            case "death" -> settingsEditor.openDeathTimerSettings(player);
            case "pylon" -> settingsEditor.openPylonFeaturesSettings(player);
            case "worldborder" -> settingsEditor.openWorldBorderSettings(player);
            case "utility" -> settingsEditor.openUtilitySettings(player);
            case "openchant" -> settingsEditor.openOpEnchantSettings(player);
            case "bossmobstrength" -> settingsEditor.openBossMobStrengthSettings(player);
            case "detailsettings" -> settingsEditor.openDetailSettingsInfo(player);
            case "resetsettings" -> settingsEditor.openInitialResetConfirmation(player);
            case "confirmreset_step2" -> settingsEditor.openFinalResetConfirmation(player);
            case "confirmreset" -> handleConfirmReset(player);
            default -> settingsEditor.openMainMenu(player);
        }
    }

    private void handleConfirmReset(Player player) {
        if (!player.hasPermission("df.admin.settings")) {
            player.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return;
        }
        configManager.resetToDefaults();
        player.sendMessage("§a모든 설정을 기본값으로 초기화했습니다. 변경사항을 적용하려면 서버를 재시작하세요.");
        settingsEditor.openMainMenu(player);
    }

    private void handleRegisterCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return;
        }

        if (!player.hasPermission("df.admin.register")) {
            player.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length < 1) {
            player.sendMessage("§c사용법: /df admin register <all|플레이어이름>");
            return;
        }

        String mode = args[0];
        if (mode.equalsIgnoreCase("all")) {
            statsManager.startMassRegistration(player);
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(mode);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage("§c'" + mode + "' 플레이어는 이 서버에 접속한 기록이 없습니다.");
                return;
            }
            statsManager.startSingleRegistration(player, target.getUniqueId());
        }
    }

    private void handleSetStatCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;
        if (args.length < 2) return;
        try {
            StatType type = StatType.valueOf(args[0].toUpperCase());
            int value = Integer.parseInt(args[1]);
            statsManager.updateStatInSession(player, type, value);
        } catch (Exception e) {
            // 채팅 클릭으로 발생하는 명령어이므로, 오류 메시지를 보내지 않습니다.
        }
    }

    private void handleConfirmStatCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        statsManager.confirmAndNext(player);
    }

    private void handleCancelStatCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        statsManager.endMassRegistration(player);
    }

    private void handleControlEnderCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("df.admin.controlender")) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§c사용법: /df admin controlender <open|openafter>");
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "open" -> {
                plugin.getEndEventManager().openEnd(true);
                sender.sendMessage("§a엔드를 즉시 개방했습니다.");
            }
            case "openafter" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c사용법: /df admin controlender openafter <분>");
                    return;
                }
                try {
                    long minutes = Long.parseLong(args[1]);
                    if (minutes <= 0) {
                        sender.sendMessage("§c시간은 1분 이상이어야 합니다.");
                        return;
                    }
                    plugin.getEndEventManager().scheduleOpen(minutes, true);
                    sender.sendMessage("§a" + minutes + "분 뒤에 엔드를 개방하도록 설정했습니다.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c시간은 숫자로 입력해야 합니다.");
                }
            }
            default -> sender.sendMessage("§c알 수 없는 명령어입니다. 사용법: /df admin controlender <open|openafter>");
        }
    }

    private void handleWeaponCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.");
            return;
        }
        if (args.length < 1) {
            player.sendMessage("§c사용법: /df admin weapon <아이템코드> [레벨]");
            return;
        }
        try {
            Material material = Material.matchMaterial(args[0].toUpperCase());
            if (material == null) {
                player.sendMessage("§c알 수 없는 아이템 코드입니다.");
                return;
            }
            int level = args.length > 1 ? Integer.parseInt(args[1]) : 0;
            ItemStack item = new ItemStack(material);

            // 아이템에 레벨을 설정하고 로어를 업데이트합니다.
            UpgradeManager upgradeManager = plugin.getUpgradeManager();
            IWeaponProfile profile = upgradeManager.getProfileRegistry().getProfile(material);
            if (profile != null) {
                upgradeManager.setUpgradeLevel(item, profile, level);
            }

            player.getInventory().addItem(item);
            player.sendMessage("§a" + material.name() + " (+" + level + ") 아이템을 지급받았습니다.");
        } catch (Exception e) {
            player.sendMessage("§c명령어 처리 중 오류가 발생했습니다.");
        }
    }

    private void handleAdminClanCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c사용법: /df admin clan <add|remove> <플레이어> [클랜]");
            return;
        }
        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c플레이어 '" + args[1] + "'를 찾을 수 없습니다.");
            return;
        }

        switch (action) {
            case "add" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c사용법: /df admin clan add <플레이어> <클랜>");
                    return;
                }
                String clanNameToAdd = args[2];
                plugin.getClanManager().forceJoinClan(target, clanNameToAdd);
                sender.sendMessage("§a" + target.getName() + "님을 " + clanNameToAdd + " 클랜에 추가했습니다.");
            }
            case "remove" -> {
                plugin.getClanManager().forceRemovePlayerFromClan(target);
                sender.sendMessage("§a" + target.getName() + "님을 클랜에서 추방했습니다.");
            }
            default -> sender.sendMessage("§c알 수 없는 명령어입니다. <add|remove>");
        }
    }
    private void handleUnbanCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("df.admin.unban")) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§c사용법: /df admin unban <플레이어>");
            return;
        }

        String playerName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§c플레이어 '" + playerName + "'는 서버에 접속한 기록이 없습니다.");
            return;
        }

        if (!plugin.getPlayerDeathManager().getDeadPlayers().containsKey(target.getUniqueId())) {
            sender.sendMessage("§c플레이어 '" + playerName + "'는 사망으로 인한 밴 상태가 아닙니다.");
            return;
        }

        plugin.getPlayerDeathManager().resurrectPlayer(target.getUniqueId());
        sender.sendMessage("§a플레이어 '" + playerName + "'의 사망 밴을 해제했습니다. 이제 접속할 수 있습니다.");
    }

    private void handleMagicStoneCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.");
            return;
        }

        int amount = 1;
        if (args.length > 0) {
            try {
                amount = Integer.parseInt(args[0]);
                if (amount <= 0) {
                    player.sendMessage("§c수량은 1 이상이어야 합니다.");
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§c올바른 숫자를 입력해주세요.");
                return;
            }
        }

        player.getInventory().addItem(MagicStone.createMagicStone(amount));
        player.sendMessage("§a마석을 " + amount + "개 지급받았습니다.");
    }
}