package cjs.DF_Plugin.pylon.beacongui.recruit;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.player.stats.PlayerStats;
import cjs.DF_Plugin.player.stats.StatType;
import cjs.DF_Plugin.player.stats.StatsEditor;
import cjs.DF_Plugin.player.stats.StatsManager;
import cjs.DF_Plugin.pylon.beacongui.BeaconGUIListener;
import cjs.DF_Plugin.pylon.config.PylonConfigManager;
import cjs.DF_Plugin.util.ItemBuilder;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RecruitGuiManager {
    private final DF_Main plugin;
    public static final String RECRUIT_GUI_TITLE = "§b[팀원 뽑기]";
    private final Set<UUID> playersInRecruitment = new HashSet<>(); // 중복 실행 방지
    private static final String PREFIX = PluginUtils.colorize("&a[팀원 뽑기] &f");

    public RecruitGuiManager(DF_Main plugin) {
        this.plugin = plugin;
    }

    public void startRecruitmentProcess(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) return;

        PylonConfigManager config = plugin.getPylonManager().getConfigManager();

        if (clan.getMembers().size() >= config.getClanMaxMembers()) {
            player.sendMessage(PREFIX + "§c가문 인원이 최대치에 도달하여 더 이상 팀원을 뽑을 수 없습니다.");
            player.closeInventory();
            return;
        }

        if (config.isRecruitRandomDraw()) {
            openInitialRecruitGui(player, clan);
        } else {
            // TODO: Implement paginated player list GUI
            player.sendMessage(PREFIX + "§c플레이어 목록을 통한 모집은 아직 구현되지 않았습니다.");
            player.closeInventory();
        }
    }

    private void openInitialRecruitGui(Player player, Clan clan) {
        Inventory gui = Bukkit.createInventory(null, 9, RECRUIT_GUI_TITLE);
        int costPerMember = plugin.getPylonManager().getConfigManager().getRecruitCostPerMember();
        int totalCost = clan.getMembers().size() * costPerMember;

        ItemStack diamond = new ItemStack(Material.DIAMOND);
        ItemMeta meta = diamond.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a팀원 무작위 뽑기");
            meta.setLore(Arrays.asList(
                    "§7클릭하여 무작위로 팀원을 뽑습니다.",
                    "§f비용: §b다이아몬드 " + totalCost + "개"
            ));
            meta.getPersistentDataContainer().set(BeaconGUIListener.GUI_BUTTON_KEY, PersistentDataType.STRING, "start_random_draw");
            diamond.setItemMeta(meta);
        }

        gui.setItem(4, diamond);
        player.openInventory(gui);
    }

    public void handleGuiClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String action = clickedItem.getItemMeta().getPersistentDataContainer().get(BeaconGUIListener.GUI_BUTTON_KEY, PersistentDataType.STRING);
        if (action == null || !action.equals("start_random_draw")) return;

        if (playersInRecruitment.contains(player.getUniqueId())) {
            return; // Already in progress
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) return;

        int costPerMember = plugin.getPylonManager().getConfigManager().getRecruitCostPerMember();
        int totalCost = clan.getMembers().size() * costPerMember;

        if (!player.getInventory().contains(Material.DIAMOND, totalCost)) {
            player.sendMessage(PREFIX + "§c팀원 뽑기에 필요한 다이아몬드가 부족합니다. (필요: " + totalCost + "개)");
            player.closeInventory();
            return;
        }

        List<UUID> recruitable = plugin.getPlayerRegistryManager().getRecruitablePlayerUUIDs();
        // Don't recruit the leader themselves
        recruitable.remove(player.getUniqueId());

        if (recruitable.isEmpty()) {
            player.sendMessage(PREFIX + "§c모집할 수 있는 플레이어가 없습니다.");
            player.closeInventory();
            return;
        }

        player.getInventory().removeItem(new ItemStack(Material.DIAMOND, totalCost));
        startSlotMachineAnimation(player, event.getInventory(), recruitable);
    }

    private void startSlotMachineAnimation(Player player, Inventory gui, List<UUID> recruitable) {
        playersInRecruitment.add(player.getUniqueId());

        new BukkitRunnable() {
            private int ticks = 0;
            private int interval = 2;
            private final int stopTick = 80; // 4 seconds
            private final Random random = new Random();

            @Override
            public void run() {
                ticks++;

                if (ticks >= stopTick) {
                    // Animation end
                    UUID finalRecruitUUID = recruitable.get(random.nextInt(recruitable.size()));
                    OfflinePlayer finalRecruit = Bukkit.getOfflinePlayer(finalRecruitUUID);

                    gui.setItem(4, createPlayerHead(finalRecruit, "§a§l" + finalRecruit.getName() + "!", "§e팀원으로 영입되었습니다!"));

                    Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                    if (clan != null) {
                        plugin.getClanManager().addMemberToClan(clan, finalRecruitUUID);
                        player.sendMessage(PREFIX + "§a" + finalRecruit.getName() + "님을 새로운 가문원으로 영입했습니다!");
                        if (finalRecruit.isOnline()) {
                            finalRecruit.getPlayer().sendMessage(PREFIX + "§a" + clan.getColor() + clan.getName() + "§a 가문에 영입되었습니다!");
                        }
                    }

                    playersInRecruitment.remove(player.getUniqueId());
                    // Close GUI after 3 seconds
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.getOpenInventory().getTitle().equals(RECRUIT_GUI_TITLE)) {
                                player.closeInventory();
                            }
                        }
                    }.runTaskLater(plugin, 60L);
                    this.cancel();
                    return;
                }

                // Slow down animation over time
                if (ticks > 60) interval = 10;
                else if (ticks > 40) interval = 5;
                else if (ticks > 20) interval = 3;

                if (ticks % interval == 0) {
                    UUID randomUUID = recruitable.get(random.nextInt(recruitable.size()));
                    OfflinePlayer randomPlayer = Bukkit.getOfflinePlayer(randomUUID);
                    gui.setItem(4, createPlayerHead(randomPlayer, "§b???", "§7누가 선택될까요..."));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private ItemStack createPlayerHead(OfflinePlayer player, String name, String... lore) {
        StatsManager statsManager = plugin.getStatsManager();
        PlayerStats stats = statsManager.getPlayerStats(player.getUniqueId());

        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD)
                .withSkullOwner(player)
                .withName(name)
                .addLoreLine(" ")
                .addLoreLine("§7" + StatType.ATTACK.getDisplayName() + ": " + StatsEditor.getStars(stats.getStat(StatType.ATTACK)))
                .addLoreLine("§7" + StatType.INTELLIGENCE.getDisplayName() + ": " + StatsEditor.getStars(stats.getStat(StatType.INTELLIGENCE)))
                .addLoreLine("§7" + StatType.STAMINA.getDisplayName() + ": " + StatsEditor.getStars(stats.getStat(StatType.STAMINA)))
                .addLoreLine("§7" + StatType.ENTERTAINMENT.getDisplayName() + ": " + StatsEditor.getStars(stats.getStat(StatType.ENTERTAINMENT)))
                .addLoreLine(" ")
                .addLoreLine("§e전투력: " + String.format("%.2f", stats.getCombatPower()));

        for (String l : lore) {
            builder.addLoreLine(l);
        }
        return builder.build();
    }
}