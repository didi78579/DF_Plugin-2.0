package cjs.DF_Plugin.player.stats;

import cjs.DF_Plugin.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StatsEditor {

    public static final String GUI_TITLE_PREFIX = "스탯 평가: ";

    public static Inventory create(Player target, PlayerStats stats) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE_PREFIX + target.getName());

        // GUI 배경 채우기
        ItemStack pane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).withName(" ").build();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }

        // 플레이어 머리
        inv.setItem(4, new ItemBuilder(Material.PLAYER_HEAD).withSkullOwner(target).withName("§f" + target.getName()).build());

        // 스탯 아이템 배치
        addStatRow(inv, 19, StatType.ATTACK, stats.getStat(StatType.ATTACK));
        addStatRow(inv, 28, StatType.INTELLIGENCE, stats.getStat(StatType.INTELLIGENCE));
        addStatRow(inv, 37, StatType.STAMINA, stats.getStat(StatType.STAMINA));
        addStatRow(inv, 46, StatType.ENTERTAINMENT, stats.getStat(StatType.ENTERTAINMENT));

        // 저장 버튼
        inv.setItem(8, new ItemBuilder(Material.WRITABLE_BOOK)
                .withName("§a저장하기")
                .withLore("§7클릭하여 현재 스탯을 저장합니다.")
                .withPDCString(StatsListener.STATS_ACTION_KEY, "SAVE").build());

        return inv;
    }

    private static void addStatRow(Inventory inv, int startSlot, StatType type, int level) {
        // 스탯 정보 아이템
        inv.setItem(startSlot, new ItemBuilder(Material.BOOK).withName("§e" + type.getDisplayName()).build());

        // 별 표시
        for (int i = 1; i <= 5; i++) {
            if (i <= level) {
                inv.setItem(startSlot + i, new ItemBuilder(Material.NETHER_STAR)
                        .withName("§e★")
                        .build());
            } else {
                inv.setItem(startSlot + i, new ItemBuilder(Material.GRAY_DYE)
                        .withName("§7☆")
                        .build());
            }
        }

        // 증감 버튼
        inv.setItem(startSlot + 7, new ItemBuilder(Material.LIME_DYE).withName("§a+")
                .withPDCString(StatsListener.STATS_ACTION_KEY, "INCREMENT")
                .withPDCString(StatsListener.STATS_TYPE_KEY, type.name()).build());
        inv.setItem(startSlot + 8, new ItemBuilder(Material.RED_DYE).withName("§c-")
                .withPDCString(StatsListener.STATS_ACTION_KEY, "DECREMENT")
                .withPDCString(StatsListener.STATS_TYPE_KEY, type.name()).build());
    }

    public static String getStars(int level) {
        StringBuilder stars = new StringBuilder();
        stars.append("§e");
        for (int i = 0; i < level; i++) stars.append("★");
        stars.append("§7");
        for (int i = 0; i < 5 - level; i++) stars.append("☆");
        return stars.toString();
    }
}