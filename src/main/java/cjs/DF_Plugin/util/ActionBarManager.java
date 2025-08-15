package cjs.DF_Plugin.util;

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

        // 충전량이 있는 능력을 먼저 표시
        Map<String, SpecialAbilityManager.ChargeInfo> charges = specialAbilityManager.getPlayerCharges(player.getUniqueId());
        if (charges != null) {
            for (Map.Entry<String, SpecialAbilityManager.ChargeInfo> entry : charges.entrySet()) {
                // 액션바에 표시하지 않도록 설정된 능력은 건너뜁니다.
                String abilityKey = entry.getKey();
                ISpecialAbility ability = specialAbilityManager.getRegisteredAbility(abilityKey);
                if (ability != null && !ability.showInActionBar()) {
                    continue;
                }

                SpecialAbilityManager.ChargeInfo info = entry.getValue();

                // 점(dot) 형태로 충전량 시각화
                StringBuilder chargeDisplay = new StringBuilder();
                chargeDisplay.append("§a"); // 사용 가능한 횟수는 녹색
                for (int i = 0; i < info.current(); i++) {
                    chargeDisplay.append("●");
                }
                chargeDisplay.append("§7"); // 사용한 횟수는 회색
                for (int i = 0; i < info.max() - info.current(); i++) {
                    chargeDisplay.append("○");
                }

                parts.add(String.format("%s %s", info.displayName(), chargeDisplay.toString()));
            }
        }

        // 전체 쿨다운 중인 능력을 나중에 표시
        Map<String, SpecialAbilityManager.CooldownInfo> cooldowns = specialAbilityManager.getPlayerCooldowns(player.getUniqueId());
        if (cooldowns != null) {
            // 원본 맵을 직접 수정하는 대신, 스트림을 사용하여 만료되지 않은 쿨다운만 필터링합니다.
            // 이는 데이터 관리와 표시 로직을 분리하여 코드의 안정성을 높입니다.
            cooldowns.entrySet().stream()
                    .filter(entry -> {
                        // 액션바에 표시하지 않도록 설정된 능력은 건너뜁니다.
                        String abilityKey = entry.getKey().split(":")[0];
                        ISpecialAbility ability = specialAbilityManager.getRegisteredAbility(abilityKey);
                        // 능력이 존재하고, 표시하도록 설정되어 있으며, 쿨다운이 만료되지 않았는지 확인합니다.
                        return ability != null && ability.showInActionBar() && entry.getValue().endTime() > currentTime;
                    })
                    .forEach(entry -> {
                SpecialAbilityManager.CooldownInfo info = entry.getValue();
                long secondsLeft = (info.endTime() - currentTime + 999) / 1000;
                parts.add(String.format("%s §7%d초", info.displayName(), secondsLeft));
            });
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