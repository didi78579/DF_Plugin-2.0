package cjs.DF_Plugin.events.end;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.enchant.MagicStone;
import cjs.DF_Plugin.items.UpgradeItems;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class EndEventManager {

    private final DF_Main plugin;
    private boolean isEndOpen;
    private long scheduledOpenTime = -1;
    private BukkitTask openTask;
    private BukkitTask collapseTask;

    private static final String CONFIG_PATH_IS_OPEN = "end-event.is-open";
    private static final String CONFIG_PATH_SCHEDULED_TIME = "end-event.scheduled-open-time";
    private static final String CONFIG_PATH_COLLAPSE_TIME = "end-event.collapse-delay-minutes";

    public EndEventManager(DF_Main plugin) {
        this.plugin = plugin;
        loadState();
    }

    public void loadState() {
        this.isEndOpen = plugin.getGameConfigManager().getConfig().getBoolean(CONFIG_PATH_IS_OPEN, false);
        this.scheduledOpenTime = plugin.getGameConfigManager().getConfig().getLong(CONFIG_PATH_SCHEDULED_TIME, -1);

        if (scheduledOpenTime != -1) {
            long delayMillis = scheduledOpenTime - System.currentTimeMillis();
            if (delayMillis <= 0) {
                // 서버가 꺼져있는 동안 열릴 시간이었으므로, 지금 바로 엽니다.
                openEnd(false);
            } else {
                // 개방 예약을 다시 설정합니다.
                scheduleOpen(TimeUnit.MILLISECONDS.toMinutes(delayMillis), false);
            }
        }
    }

    private void saveState() {
        plugin.getGameConfigManager().getConfig().set(CONFIG_PATH_IS_OPEN, isEndOpen);
        plugin.getGameConfigManager().getConfig().set(CONFIG_PATH_SCHEDULED_TIME, scheduledOpenTime);
        plugin.getGameConfigManager().save();
    }

    public void openEnd(boolean broadcast) {
        if (isEndOpen) return;

        if (openTask != null) {
            openTask.cancel();
            openTask = null;
        }

        this.isEndOpen = true;
        this.scheduledOpenTime = -1;
        saveState();

        if (broadcast) {
            Bukkit.broadcastMessage("§5[엔드 이벤트] §d엔더 드래곤의 포효가 들려옵니다! 엔드 포탈이 활성화되었습니다!");
        }
        plugin.getLogger().info("The End has been opened.");
    }

    public void scheduleOpen(long minutes, boolean broadcast) {
        if (minutes <= 0) {
            openEnd(true);
            return;
        }

        if (openTask != null) {
            openTask.cancel();
        }

        this.isEndOpen = false;
        this.scheduledOpenTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes);
        saveState();

        if (broadcast) {
            Bukkit.broadcastMessage("§5[엔드 이벤트] §d공허의 기운이 꿈틀거립니다... " + minutes + "분 뒤 엔드 포탈이 열립니다!");
        }

        openTask = Bukkit.getScheduler().runTaskLater(plugin, () -> openEnd(true), minutes * 60 * 20L);
        plugin.getLogger().info("The End is scheduled to open in " + minutes + " minutes.");
    }

    public boolean isEndOpen() {
        return isEndOpen;
    }

    /**
     * 엔더 드래곤 처치 시 보상 지급 및 월드 붕괴 절차를 시작합니다.
     */
    public void triggerDragonDefeatSequence() {
        if (!isEndOpen) return;

        World endWorld = Bukkit.getWorld("world_the_end");
        if (endWorld != null) {
            scatterRewards(endWorld);
            Bukkit.broadcastMessage("§5[엔드 이벤트] §d엔더 드래곤이 쓰러졌습니다! 하늘에서 보상이 쏟아집니다!");
        } else {
            plugin.getLogger().log(Level.WARNING, "엔드 월드가 로드되지 않아 보상을 지급할 수 없습니다.");
        }

        long delayMinutes = plugin.getGameConfigManager().getConfig().getLong(CONFIG_PATH_COLLAPSE_TIME, 10);
        Bukkit.broadcastMessage("§5[엔드 이벤트] §c엔드 월드가 " + delayMinutes + "분 뒤 붕괴를 시작합니다! 서둘러 탈출하세요!");

        if (collapseTask != null) {
            collapseTask.cancel();
        }
        collapseTask = Bukkit.getScheduler().runTaskLater(plugin, this::closeAndResetEnd, delayMinutes * 60 * 20L);
    }

    private void closeAndResetEnd() {
        if (collapseTask != null) {
            collapseTask.cancel();
            collapseTask = null;
        }

        World endWorld = Bukkit.getWorld("world_the_end");
        if (endWorld != null) {
            for (Player player : endWorld.getPlayers()) {
                teleportPlayerToSafety(player);
            }
        }

        this.isEndOpen = false;
        this.scheduledOpenTime = -1;
        saveState();

        Bukkit.broadcastMessage("§5[엔드 이벤트] §4엔드 월드가 붕괴하여 닫혔습니다.");
        plugin.getLogger().info("The End has been closed and is being reset.");

        plugin.getWorldManager().resetWorld("world_the_end");
    }

    public void teleportPlayerToSafety(Player player) {
        plugin.getWorldManager().teleportPlayerToSafety(player);
    }

    /**
     * 엔더 드래곤 처치 후 설정된 위치에 보상을 뿌립니다.
     * @param world 보상을 뿌릴 월드 (엔드 월드)
     */
    public void scatterRewards(World world) {
        FileConfiguration config = plugin.getGameConfigManager().getConfig();
        if (world.getEnvironment() != World.Environment.THE_END) {
            plugin.getLogger().warning("보상 뿌리기는 엔드 월드에서만 가능합니다.");
            return;
        }

        // 설정 값 읽기
        int areaSize = config.getInt("end-event.rewards.area-size", 100);
        int dropY = config.getInt("end-event.rewards.drop-y-level", 200);
        int minStack = config.getInt("end-event.rewards.min-stack-size", 2);
        int maxStack = config.getInt("end-event.rewards.max-stack-size", 4);
        int minTotal = config.getInt("end-event.rewards.min-total-quantity", 1000);
        int maxTotal = config.getInt("end-event.rewards.max-total-quantity", 2000);

        int totalItemsToDrop = ThreadLocalRandom.current().nextInt(minTotal, maxTotal + 1);
        int itemsDropped = 0;

        plugin.getLogger().info(String.format("엔더 드래곤 보상을 뿌립니다. 총 %d개", totalItemsToDrop));

        while (itemsDropped < totalItemsToDrop) {
            // 랜덤 위치 생성
            double x = ThreadLocalRandom.current().nextDouble(-areaSize / 2.0, areaSize / 2.0);
            double z = ThreadLocalRandom.current().nextDouble(-areaSize / 2.0, areaSize / 2.0);
            Location dropLocation = new Location(world, x, dropY, z);

            // 랜덤 스택 크기 결정
            int stackSize = ThreadLocalRandom.current().nextInt(minStack, maxStack + 1);
            if (itemsDropped + stackSize > totalItemsToDrop) {
                stackSize = totalItemsToDrop - itemsDropped;
            }

            // 50% 확률로 마석 또는 강화석 선택
            ItemStack rewardItem;
            if (ThreadLocalRandom.current().nextBoolean()) {
                rewardItem = MagicStone.createMagicStone(stackSize);
            } else {
                rewardItem = UpgradeItems.createUpgradeStone(stackSize);
            }

            // 아이템 드롭 및 발광 효과 적용
            Item droppedItemEntity = world.dropItemNaturally(dropLocation, rewardItem);
            droppedItemEntity.setGlowing(true);
            droppedItemEntity.setUnlimitedLifetime(true); // 아이템이 사라지지 않도록 설정

            itemsDropped += stackSize;
        }
    }
}