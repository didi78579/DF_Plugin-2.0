package cjs.DF_Plugin.player.stats;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * 스탯 평가를 위한 클릭 가능한 채팅 UI를 생성하는 클래스입니다.
 */
public class StatsChatEditor {

    public static void sendEditor(Player editor, OfflinePlayer target, PlayerStats stats, String progress) {
        editor.sendMessage("§7§m-----------------------------------------");
        editor.sendMessage(" §b스탯 평가: §f" + target.getName() + " " + progress);
        editor.sendMessage(" ");

        for (StatType type : StatType.values()) {
            editor.spigot().sendMessage(createStatLine(type, stats.getStat(type)));
        }

        editor.sendMessage(" ");
        editor.spigot().sendMessage(createControls());
        editor.sendMessage("§7§m-----------------------------------------");
    }

    private static TextComponent createStatLine(StatType type, int level) {
        TextComponent line = new TextComponent(" §e" + String.format("%-4s", type.getDisplayName()) + " : ");

        for (int i = 1; i <= 5; i++) {
            TextComponent star = new TextComponent(i <= level ? "§e★" : "§7☆");
            star.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/df setstat " + type.name() + " " + i));
            star.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§f" + i + "점 부여").create()));
            line.addExtra(star);
            line.addExtra(" ");
        }
        return line;
    }

    private static TextComponent createControls() {
        TextComponent controls = new TextComponent(" ");

        TextComponent confirmButton = new TextComponent("§a§l[확정 및 다음]");
        confirmButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/df confirmstat"));
        confirmButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§f현재 스탯을 저장하고 다음 플레이어로 넘어갑니다.").create()));

        TextComponent spacer = new TextComponent("  ");

        TextComponent cancelButton = new TextComponent("§c§l[취소]");
        cancelButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/df cancelstat"));
        cancelButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§f스탯 평가를 중단합니다.").create()));

        controls.addExtra(confirmButton);
        controls.addExtra(spacer);
        controls.addExtra(cancelButton);

        return controls;
    }
}