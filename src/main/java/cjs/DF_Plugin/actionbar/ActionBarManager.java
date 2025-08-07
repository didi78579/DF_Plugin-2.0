package cjs.DF_Plugin.actionbar;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.specialability.ISpecialAbility;
import cjs.DF_Plugin.upgrade.specialability.SpecialAbilityManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ActionBarManager {

    private final DF_Main plugin;
    private final SpecialAbilityManager specialAbilityManager;

    public ActionBarManager(DF_Main plugin, SpecialAbilityManager specialAbilityManager) {
        this.plugin = plugin;
        this.specialAbilityManager = specialAbilityManager;
        startUpdater();
    }

    private void startUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateActionBar(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L); // 0.25초마다 업데이트
    }

    private void updateActionBar(Player player) {
        List<String> parts = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        Map<String, SpecialAbilityManager.CooldownInfo> cooldowns = specialAbilityManager.getPlayerCooldowns(player.getUniqueId());

        // 쿨다운 정보 표시
        if (cooldowns != null) {
            cooldowns.entrySet().removeIf(entry -> entry.getValue().endTime() <= currentTime);
            for (Map.Entry<String, SpecialAbilityManager.CooldownInfo> entry : cooldowns.entrySet()) {
                SpecialAbilityManager.CooldownInfo info = entry.getValue();
                long timeLeft = info.endTime() - currentTime;
                if (timeLeft > 0) {
                    long secondsLeft = (timeLeft + 999) / 1000;
                    parts.add(String.format("%s §e%d초", info.displayName(), secondsLeft));
                }
            }
        }

        if (!parts.isEmpty()) {
            String message = String.join("  §7|  ", parts);
            sendActionBar(player, message);
        }
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || !player.isOnline()) return;
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
}