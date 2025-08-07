package cjs.DF_Plugin.clan.registration;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.clan.ClanManager;
import cjs.DF_Plugin.pylon.item.PylonItem;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ClanRegistrationManager {
    private final DF_Main plugin;
    private final ClanManager clanManager;
    private static final String PREFIX = PluginUtils.colorize("&a[클랜] &f");

    public ClanRegistrationManager(DF_Main plugin, ClanManager clanManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
    }

    public void createClan(Player leader, String clanName, ChatColor color) {
        // 유효성 검사
        if (clanManager.getClanByPlayer(leader.getUniqueId()) != null) {
            leader.sendMessage(PREFIX + "§c이미 다른 가문에 소속되어 있습니다.");
            return;
        }
        if (clanName == null || clanName.length() < 2 || clanName.length() > 10) {
            leader.sendMessage(PREFIX + "§c가문 이름은 2~10글자 사이여야 합니다.");
            return;
        }
        if (clanManager.isNameTaken(clanName)) {
            leader.sendMessage(PREFIX + "§c이미 사용 중인 가문 이름입니다.");
            return;
        }
        if (clanManager.isColorTaken(color)) {
            leader.sendMessage(PREFIX + "§c이미 사용 중인 가문 색상입니다.");
            return;
        }

        // 가문 생성 및 저장
        Clan newClan = clanManager.createClan(clanName, leader, color);

        leader.getInventory().addItem(PylonItem.createPylonItem());

        leader.sendMessage(PREFIX + "§a가문 '" + color + clanName + "§a'이(가) 성공적으로 생성되었습니다!");
    }

    public void disbandClan(Player player) {
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(PREFIX + "§c소속된 가문이 없습니다.");
            return;
        }
        if (!clan.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(PREFIX + "§c가문장만 가문을 해체할 수 있습니다.");
            return;
        }

        clanManager.disbandClan(clan);
        player.sendMessage(PREFIX + "§a가문 '" + clan.getColor() + clan.getName() + "§a'이(가) 해체되었습니다.");
    }
}