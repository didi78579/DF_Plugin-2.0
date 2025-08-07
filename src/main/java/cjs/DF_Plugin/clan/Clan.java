package cjs.DF_Plugin.clan;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 클랜의 모든 데이터를 담는 클래스
 */
public class Clan {

    private final String name;
    private ChatColor color;
    private UUID leader;
    private final Set<UUID> members;

    // 파일런 및 부가 기능 데이터
    private final List<String> pylonLocations = new ArrayList<>();
    private long lastRetrievalTime;
    private long lastReconFireworkTime;
    private long lastGiftBoxTime;
    private boolean giftBoxReady;

    public Clan(String name, UUID leader, ChatColor color) {
        this.name = name;
        this.leader = leader;
        this.color = color;
        this.members = new HashSet<>();
        this.members.add(leader);
    }

    /**
     * 설정 파일(YAML)로부터 클랜 데이터를 불러올 때 사용하는 생성자
     */
    public Clan(String name, ConfigurationSection config) {
        this.name = name;
        this.leader = UUID.fromString(config.getString("leader"));
        this.color = ChatColor.valueOf(config.getString("color", "WHITE"));
        List<String> memberUuids = config.getStringList("members");
        this.members = memberUuids.stream().map(UUID::fromString).collect(Collectors.toSet());

        // 파일런 및 부가 기능 데이터 로드
        this.pylonLocations.addAll(config.getStringList("pylons"));
        this.lastRetrievalTime = config.getLong("last-retrieval-time", 0);
        this.lastReconFireworkTime = config.getLong("last-recon-firework-time", 0);
        this.lastGiftBoxTime = config.getLong("last-gift-box-time", 0);
        this.giftBoxReady = config.getBoolean("gift-box-ready", false);
    }

    /**
     * 클랜 데이터를 설정 파일에 저장
     */
    public void save(ConfigurationSection config) {
        config.set("leader", leader.toString());
        config.set("color", color.name());
        config.set("members", members.stream().map(UUID::toString).collect(Collectors.toList()));
        config.set("pylons", pylonLocations);
        config.set("last-retrieval-time", lastRetrievalTime);
        config.set("last-recon-firework-time", lastReconFireworkTime);
        config.set("last-gift-box-time", lastGiftBoxTime);
        config.set("gift-box-ready", giftBoxReady);
    }

    public String getName() {
        return name;
    }

    public ChatColor getColor() {
        return color;
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID newLeader) {
        this.leader = newLeader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID uuid) { members.add(uuid); }

    public void removeMember(UUID uuid) { members.remove(uuid); }

    public void broadcastMessage(String message) {
        members.stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline())
                .forEach(player -> player.sendMessage(message));
    }

    // --- Pylon and Feature Getters/Setters ---

    public void addPylonLocation(String location) { pylonLocations.add(location); }

    public List<String> getPylonLocations() { return pylonLocations; }

    public long getLastRetrievalTime() { return lastRetrievalTime; }
    public void setLastRetrievalTime(long time) { this.lastRetrievalTime = time; }

    public long getLastReconFireworkTime() { return lastReconFireworkTime; }
    public void setLastReconFireworkTime(long time) { this.lastReconFireworkTime = time; }

    public long getLastGiftBoxTime() { return lastGiftBoxTime; }
    public void setLastGiftBoxTime(long time) { this.lastGiftBoxTime = time; }

    public boolean isGiftBoxReady() { return giftBoxReady; }
    public void setGiftBoxReady(boolean ready) { this.giftBoxReady = ready; }
}