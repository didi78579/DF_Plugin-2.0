package cjs.DF_Plugin.command;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.command.editor.SettingsEditor;
import cjs.DF_Plugin.misc.RecipeManager;
import cjs.DF_Plugin.world.WorldManager;
import cjs.DF_Plugin.settings.GameConfigManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetCommand {

    private final GameConfigManager configManager;
    private final SettingsEditor settingsEditor;
    private final WorldManager worldManager;
    private final RecipeManager recipeManager;

    public SetCommand(DF_Main plugin) {
        this.configManager = plugin.getGameConfigManager();
        this.settingsEditor = new SettingsEditor(plugin);
        this.worldManager = plugin.getWorldManager();
        this.recipeManager = plugin.getRecipeManager();
    }

    public void handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("df.admin.settings")) {
            player.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§c사용법: /df set <설정키> <값>");
            return;
        }

        String key = args[1];
        String valueStr = args[2];
        Object currentValue = configManager.getConfig().get(key);

        if (currentValue == null) {
            player.sendMessage("§c'" + key + "'는 존재하지 않는 설정입니다.");
            return;
        }

        try {
            if (currentValue instanceof Integer) {
                configManager.set(key, Integer.parseInt(valueStr));
            } else if (currentValue instanceof Double) {
                configManager.set(key, Double.parseDouble(valueStr));
            } else if (currentValue instanceof Boolean) {
                configManager.set(key, Boolean.parseBoolean(valueStr));
            } else {
                configManager.set(key, valueStr);
            }
            configManager.save();
            player.sendMessage("§a설정 '" + key + "'을(를) '" + valueStr + "'(으)로 변경했습니다.");

            // 특정 설정은 즉시 적용 로직 호출
            if (key.startsWith("utility.location-info-disabled") || key.startsWith("worldborder.")) {
                worldManager.applyAllWorldSettings();
            } else if (key.equals("utility.notched-apple-recipe")) {
                recipeManager.updateRecipes();
            }

            // 설정 변경 후, 해당 카테고리의 설정창을 다시 열어줍니다.
            refreshSettingsUI(player, key);

        } catch (NumberFormatException e) {
            player.sendMessage("§c잘못된 숫자 형식입니다.");
        }
    }

    private void refreshSettingsUI(Player player, String key) {
        String category = key.split("\\.")[0];
        switch (category) {
            case "death-timer":
                settingsEditor.openDeathTimerSettings(player);
                break;
            case "pylon-features":
                settingsEditor.openPylonFeaturesSettings(player);
                break;
            case "worldborder":
                settingsEditor.openWorldBorderSettings(player);
                break;
            case "utility":
                settingsEditor.openUtilitySettings(player);
                break;
            case "op-enchant":
                settingsEditor.openOpEnchantSettings(player);
                break;
            case "boss-mob-strength":
                settingsEditor.openBossMobStrengthSettings(player);
                break;
            default:
                settingsEditor.openMainMenu(player);
                break;
        }
    }
}