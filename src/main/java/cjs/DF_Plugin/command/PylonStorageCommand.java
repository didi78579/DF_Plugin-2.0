package cjs.DF_Plugin.command;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.clan.ClanManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PylonStorageCommand implements CommandExecutor {

    private final ClanManager clanManager;

    public PylonStorageCommand(DF_Main plugin) {
        this.clanManager = plugin.getClanManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§c당신은 가문에 소속되어 있지 않습니다.");
            return true;
        }

        clanManager.openPylonStorage(player);
        return true;
    }
}