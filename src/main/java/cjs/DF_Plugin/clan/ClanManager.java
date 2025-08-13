package cjs.DF_Plugin.clan;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.ui.ClanUIManager;
import cjs.DF_Plugin.clan.storage.ClanStorageManager;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ClanManager {

    private final DF_Main plugin;
    private final ClanStorageManager storageManager;
    private final PlayerTagManager playerTagManager;

    // UI 세션 관리를 위한 맵
    private final Map<UUID, CreationSession> creationSessions = new HashMap<>();
    private final Map<UUID, String> deletionConfirmations = new HashMap<>();
    // Map<클랜이름 (소문자), Clan>
    private final Map<String, Clan> clans = new HashMap<>();
    // Map<플레이어UUID, Clan>
    private final Map<UUID, Clan> playerClanMap = new HashMap<>();

    public ClanManager(DF_Main plugin) {
        this.plugin = plugin;
        this.storageManager = new ClanStorageManager(plugin);
        this.playerTagManager = new PlayerTagManager(plugin, this);
        loadClans();
    }

    /**
     * StorageManager를 통해 파일에서 모든 클랜 데이터를 불러와 캐시에 저장합니다.
     */
    public void loadClans() {
        clans.clear();
        playerClanMap.clear();
        Map<String, Clan> loadedClans = storageManager.loadAllClans();
        clans.putAll(loadedClans);
        clans.values().forEach(clan ->
                clan.getMembers().forEach(memberId -> playerClanMap.put(memberId, clan))
        );
        plugin.getLogger().info("ClanManager loaded with " + clans.size() + " clans.");
    }

    public Clan createClan(String name, Player leader, ChatColor color) {
        Clan clan = new Clan(name, leader.getUniqueId(), color);
        clans.put(name.toLowerCase(), clan);
        playerClanMap.put(leader.getUniqueId(), clan);
        storageManager.saveClan(clan);
        return clan;
    }

    public void disbandClan(Clan clan) {
        clan.broadcastMessage(PluginUtils.colorize("&a[클랜] &f클랜이 리더에 의해 해체되었습니다."));
        // Make a copy to avoid ConcurrentModificationException if playerClanMap is modified elsewhere
        Set<UUID> members = new HashSet<>(clan.getMembers());
        members.forEach(memberId -> {
            playerClanMap.remove(memberId);
            plugin.getPlayerRegistryManager().updatePlayerClan(memberId, null);
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                playerTagManager.removePlayerTag(player);
            }
        });
        clans.remove(clan.getName().toLowerCase());
        storageManager.deleteClan(clan.getName());
        playerTagManager.cleanupClanTeam(clan);
    }

    public void absorbClan(Clan attacker, Clan defender) {
        // Notify defender's members about absorption
        defender.broadcastMessage(PluginUtils.colorize("&c[전쟁] &f가문이 멸망하여 " + attacker.getColor() + attacker.getName() + "&f 가문에 흡수되었습니다."));

        Set<UUID> membersToAbsorb = new HashSet<>(defender.getMembers());
        for (UUID memberUUID : membersToAbsorb) {
            attacker.addMember(memberUUID);
            plugin.getPlayerRegistryManager().updatePlayerClan(memberUUID, attacker);
            playerClanMap.put(memberUUID, attacker); // Update player's clan mapping
            Player onlineMember = Bukkit.getPlayer(memberUUID);
            if (onlineMember != null) {
                playerTagManager.updatePlayerTag(onlineMember); // Update scoreboard tag for online members
            }
        }

        // Save the state of the victorious clan
        storageManager.saveClan(attacker);

        // Remove the defeated clan from the system
        clans.remove(defender.getName().toLowerCase());
        storageManager.deleteClan(defender.getName());
        playerTagManager.cleanupClanTeam(defender);
    }

    public void addPlayerToClan(Player player, Clan clan) {
        clan.addMember(player.getUniqueId());
        playerClanMap.put(player.getUniqueId(), clan);
        storageManager.saveClan(clan);
        playerTagManager.updatePlayerTag(player);
        plugin.getPlayerRegistryManager().updatePlayerClan(player.getUniqueId(), clan);
    }

    public void addMemberToClan(Clan clan, UUID memberUUID) {
        clan.addMember(memberUUID);
        playerClanMap.put(memberUUID, clan);
        storageManager.saveClan(clan);
        Player onlineMember = Bukkit.getPlayer(memberUUID);
        if (onlineMember != null) {
            playerTagManager.updatePlayerTag(onlineMember);
        }
        plugin.getPlayerRegistryManager().updatePlayerClan(memberUUID, clan);
    }

    public void removePlayerFromClan(Player player, Clan clan) {
        clan.removeMember(player.getUniqueId());
        playerClanMap.remove(player.getUniqueId());
        // 리더가 나갔을 경우 처리 (예: 다른 멤버에게 리더 위임) 로직 추가 필요
        storageManager.saveClan(clan);
        playerTagManager.removePlayerTag(player);
        plugin.getPlayerRegistryManager().updatePlayerClan(player.getUniqueId(), null);
    }

    public Clan getClanByName(String name) { return clans.get(name.toLowerCase()); }
    public Clan getClanByPlayer(UUID uuid) { return playerClanMap.get(uuid); }
    public ClanStorageManager getStorageManager() { return storageManager; }
    public PlayerTagManager getPlayerTagManager() { return playerTagManager; }

    /**
     * 파일런 위치 문자열로 해당 파일런을 소유한 클랜을 찾습니다.
     * @param locationStr 직렬화된 위치 문자열
     * @return 해당 위치에 파일런이 있는 클랜 (Optional)
     */
    public Optional<Clan> getClanByPylonLocation(String locationStr) {
        return clans.values().stream()
                .filter(clan -> clan.getPylonLocations().contains(locationStr))
                .findFirst();
    }
    public Collection<Clan> getClans() { return clans.values(); }

    /**
     * 관리자가 플레이어를 클랜에 강제로 추가합니다.
     * 플레이어가 이미 다른 클랜에 속해 있다면, 이전 클랜에서 자동으로 제거됩니다.
     * @param player 대상 플레이어
     * @param clanName 클랜 이름
     */
    public void forceAddPlayerToClan(Player player, String clanName) {
        Clan targetClan = getClanByName(clanName);
        if (targetClan == null) {
            // 호출한 쪽에서 클랜이 없는 경우를 처리해야 합니다.
            return;
        }

        Clan currentClan = getClanByPlayer(player.getUniqueId());
        if (currentClan != null) {
            if (currentClan.equals(targetClan)) {
                return; // 이미 해당 클랜 소속입니다.
            }
            // 기존 클랜에서 강제 제거합니다.
            forceRemovePlayerFromClan(player);
        }

        // 새 클랜에 추가합니다.
        addPlayerToClan(player, targetClan);
    }

    /**
     * 관리자가 플레이어를 클랜에서 강제로 제거합니다.
     * 만약 제거되는 플레이어가 리더일 경우, 클랜은 자동으로 해체됩니다.
     * @param player 대상 플레이어
     */
    public void forceRemovePlayerFromClan(Player player) {
        Clan clan = getClanByPlayer(player.getUniqueId());
        if (clan == null) return;

        // 제거 대상이 리더인 경우, 클랜을 해체하여 리더 없는 클랜이 생기는 것을 방지합니다.
        if (clan.getLeader().equals(player.getUniqueId())) {
            disbandClan(clan);
        } else {
            // 일반 멤버는 기존의 탈퇴 로직을 따릅니다.
            removePlayerFromClan(player, clan);
        }
    }

    public List<String> getClanNames() {
        return clans.values().stream().map(Clan::getName).collect(Collectors.toList());
    }

    public boolean isNameTaken(String name) {
        return clans.containsKey(name.toLowerCase());
    }

    public boolean isColorTaken(ChatColor color) {
        return clans.values().stream().anyMatch(clan -> clan.getColor() == color);
    }

    public List<ChatColor> getAvailableColors() {
        Set<ChatColor> usedColors = clans.values().stream()
                .map(Clan::getColor)
                .collect(Collectors.toSet());

        return Arrays.stream(ChatColor.values())
                .filter(ChatColor::isColor)
                .filter(c -> c != ChatColor.BLACK && c != ChatColor.DARK_GRAY && c != ChatColor.GRAY && c != ChatColor.WHITE)
                .filter(c -> !usedColors.contains(c))
                .collect(Collectors.toList());
    }

    // --- UI Session Management ---

    public CreationSession startCreationSession(Player player) {
        List<ChatColor> availableColors = getAvailableColors();
        if (availableColors.isEmpty()) {
            return null;
        }
        CreationSession session = new CreationSession(availableColors);
        creationSessions.put(player.getUniqueId(), session);
        return session;
    }

    public CreationSession getCreationSession(Player player) {
        return creationSessions.get(player.getUniqueId());
    }

    public void endCreationSession(Player player) {
        creationSessions.remove(player.getUniqueId());
    }

    public void requestDeletion(Player player, Clan clan) {
        deletionConfirmations.put(player.getUniqueId(), clan.getName());
    }

    public String getDeletionConfirmation(Player player) {
        return deletionConfirmations.get(player.getUniqueId());
    }

    public void clearDeletionConfirmation(Player player) {
        deletionConfirmations.remove(player.getUniqueId());
    }

    public static class CreationSession {
        public String name;
        public ChatColor color;
        private int colorIndex = 0;
        private final List<ChatColor> availableColors;

        public CreationSession(List<ChatColor> availableColors) {
            this.availableColors = availableColors;
            this.color = availableColors.get(0);
        }

        public void nextColor() {
            if (availableColors.isEmpty()) return;
            colorIndex = (colorIndex + 1) % availableColors.size();
            color = availableColors.get(colorIndex);
        }

        public void prevColor() {
            if (availableColors.isEmpty()) return;
            colorIndex = (colorIndex - 1 + availableColors.size()) % availableColors.size();
            color = availableColors.get(colorIndex);
        }
    }
}