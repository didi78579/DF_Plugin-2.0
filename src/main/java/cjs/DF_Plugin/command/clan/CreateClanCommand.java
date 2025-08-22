package cjs.DF_Plugin.command.clan;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.pylon.clan.ClanManager;
import cjs.DF_Plugin.pylon.clan.Clan;
import cjs.DF_Plugin.util.item.PylonItemFactory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Arrays;

public class CreateClanCommand {

    private final DF_Main plugin;
    private final ClanManager clanManager;
    private final ClanUIManager uiManager;

    public CreateClanCommand(DF_Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.uiManager = plugin.getClanManager().getUiManager();
    }

    public boolean handle(@NotNull Player player, @NotNull String[] args) {
        if (clanManager.getClanByPlayer(player.getUniqueId()) != null) {
            player.sendMessage("§c[가문] §c이미 가문에 소속되어 있습니다.");
            return true;
        }

        ClanManager.CreationSession session = clanManager.getCreationSession(player);
        if (session == null) {
            session = clanManager.startCreationSession(player);
            if (session == null) {
                player.sendMessage("§c[가문] §c생성 가능한 색상이 모두 사용 중입니다.");
                return true;
            }
        } else {
            // 세션이 이미 존재하면, 최신 정보로 갱신합니다.
            // 다른 플레이어가 그 사이에 가문을 생성하여 사용 가능한 색상이 변경되었을 수 있습니다.
            List<org.bukkit.ChatColor> currentAvailableColors = clanManager.getAvailableColors();
            if (currentAvailableColors.isEmpty()) {
                player.sendMessage("§c[가문] §c생성 가능한 색상이 모두 사용 중입니다.");
                clanManager.endCreationSession(player);
                return true;
            }
            session.refreshAvailableColors(currentAvailableColors);
        }

        if (args.length == 0) {
            uiManager.openCreationUI(player, session);
            return true;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "setname":
                if (args.length < 2) {
                    player.sendMessage("§c[가문] §c사용법: /df clan create setname <이름>");
                    return true;
                }
                String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (clanManager.isNameTaken(name)) {
                    player.sendMessage("§c[가문] §c이미 사용 중인 가문 이름입니다.");
                    return true;
                }
                session.name = name;
                break;
            case "color":
                if (args.length < 2) {
                    uiManager.openCreationUI(player, session);
                    return true;
                }
                if (args[1].equalsIgnoreCase("next")) {
                    session.nextColor();
                } else if (args[1].equalsIgnoreCase("prev")) {
                    session.prevColor();
                }
                break;
            case "confirm":
                if (session.name == null || session.name.isEmpty()) {
                    player.sendMessage("§c[가문] §c가문 이름을 먼저 설정해주세요.");
                    return true;
                }
                Clan newClan = clanManager.createClan(session.name, player, session.color);
                player.sendMessage("§a[가문] §a가문 '" + session.color + session.name + "§a'이(가) 성공적으로 생성되었습니다!");

                // 주 파일런 코어 지급
                player.getInventory().addItem(PylonItemFactory.createMainCore());
                // 플레이어 태그 즉시 업데이트
                clanManager.getPlayerTagManager().updatePlayerTag(player);

                clanManager.endCreationSession(player);
                return true;
            default:
                player.sendMessage("§c[가문] §c알 수 없는 명령어입니다.");
                break;
        }

        uiManager.openCreationUI(player, session);
        return true;
    }
}