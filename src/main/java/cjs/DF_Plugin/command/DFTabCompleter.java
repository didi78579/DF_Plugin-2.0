package cjs.DF_Plugin.command;

import cjs.DF_Plugin.DF_Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DFTabCompleter implements TabCompleter {

    private final DF_Main plugin;

    public DFTabCompleter(DF_Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        // /df <subcommand>
        if (args.length == 1) {
            suggestions.addAll(Arrays.asList("createclan", "deleteclan"));

            // 관리자 명령어는 권한이 있을 때만 추가
            if (sender.hasPermission("df.admin")) {
                suggestions.add("admin");
            }
        } else if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();

            if ("admin".equals(subCommand)) {
                handleAdminTabComplete(sender, args, suggestions);
            }
        }

        StringUtil.copyPartialMatches(args[args.length - 1], suggestions, completions);
        Collections.sort(completions);
        return completions;
    }

    private void handleAdminTabComplete(CommandSender sender, String[] args, List<String> suggestions) {
        if (!sender.hasPermission("df.admin")) {
            return;
        }
        if (args.length == 2) {
            suggestions.addAll(Arrays.asList("weapon", "clan"));
        } else {
            String adminAction = args[1].toLowerCase();
            if ("weapon".equals(adminAction)) {
                if (args.length == 3) {
                    // 강화 가능한 아이템 목록 제안
                    suggestions.addAll(
                            Arrays.stream(Material.values())
                                    .filter(m -> plugin.getWeaponProfileManager().getProfile(m) != null)
                                    .map(m -> m.name().toLowerCase())
                                    .collect(Collectors.toList())
                    );
                } else if (args.length == 4) {
                    suggestions.addAll(Arrays.asList("1", "5", "10"));
                }
            } else if ("clan".equals(adminAction)) {
                if (args.length == 3) {
                    suggestions.addAll(Arrays.asList("add", "remove"));
                } else if (args.length == 4) {
                    // 온라인 플레이어 이름 제안
                    suggestions.addAll(
                            Bukkit.getOnlinePlayers().stream()
                                    .map(Player::getName)
                                    .collect(Collectors.toList())
                    );
                } else if (args.length == 5 && "add".equalsIgnoreCase(args[2])) {
                    suggestions.addAll(plugin.getClanManager().getClanNames());
                }
            }
        }
    }
}