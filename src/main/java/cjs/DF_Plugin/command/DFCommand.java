package cjs.DF_Plugin.command;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.command.editor.SettingsEditor;
import cjs.DF_Plugin.settings.GameConfigManager;
import cjs.DF_Plugin.settings.GameModeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

public class DFCommand implements CommandExecutor, TabCompleter {

    private final DF_Main plugin;
    private final GameModeManager gameModeManager;
    private final GameConfigManager configManager;
    private final SettingsEditor settingsEditor;
    private final SetCommand setCommand;
    private final HashMap<UUID, Long> resetConfirm = new HashMap<>();

    public DFCommand(DF_Main plugin) {
        this.plugin = plugin;
        this.gameModeManager = plugin.getGameModeManager();
        this.configManager = plugin.getGameConfigManager();
        this.settingsEditor = new SettingsEditor(plugin);
        this.setCommand = new SetCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c사용법: /df <subcommand>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "gamemode":
                handleGameModeCommand(sender, args);
                break;
            case "settings":
                handleSettingsCommand(sender, args);
                break;
            case "set":
                setCommand.handle(sender, args);
                break;
            default:
                sender.sendMessage("§c알 수 없는 명령어입니다.");
                break;
        }

        return true;
    }

    private void handleGameModeCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("df.admin.gamemode")) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§c사용법: /df gamemode <darkforest|pylon|upgrade>");
            return;
        }

        String mode = args[1].toLowerCase();
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

        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return;
        }
        Player player = (Player) sender;

        if (args.length == 1) {
            settingsEditor.openMainMenu(player);
            return;
        }

        String category = args[1].toLowerCase();
        switch (category) {
            case "death":
                settingsEditor.openDeathTimerSettings(player);
                break;
            case "pylon":
                settingsEditor.openPylonFeaturesSettings(player);
                break;
            case "worldborder":
                settingsEditor.openWorldBorderSettings(player);
                break;
            case "utility":
                settingsEditor.openUtilitySettings(player);
                break;
            case "openchant":
                settingsEditor.openOpEnchantSettings(player);
                break;
            case "bossmobstrength":
                settingsEditor.openBossMobStrengthSettings(player);
                break;
            case "detailsettings":
                settingsEditor.openDetailSettingsInfo(player);
                break;
            case "resetsettings":
                handleResetSettings(player);
                break;
            default:
                settingsEditor.openMainMenu(player);
                break;
        }
    }

    private void handleResetSettings(Player player) {
        if (resetConfirm.containsKey(player.getUniqueId()) && (System.currentTimeMillis() - resetConfirm.get(player.getUniqueId())) < 10000) {
            configManager.resetToDefaults();
            player.sendMessage("§a모든 설정을 기본값으로 초기화했습니다. 변경사항을 적용하려면 서버를 재시작하세요.");
            resetConfirm.remove(player.getUniqueId());
        } else {
            player.sendMessage("§c경고! 모든 설정을 기본값으로 초기화합니다. 실행하려면 10초 안에 이 명령어를 다시 입력하세요.");
            resetConfirm.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("gamemode", "settings", "set")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("gamemode")) {
                return Arrays.asList("darkforest", "pylon", "upgrade")
                        .stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("settings")) {
                return Arrays.asList("death", "pylon", "worldborder", "utility", "openchant", "bossmobstrength", "detailsettings", "resetsettings")
                        .stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }
        return null;
    }
}