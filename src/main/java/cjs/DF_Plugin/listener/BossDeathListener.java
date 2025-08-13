package cjs.DF_Plugin.listener;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.items.SpecialItemFactory;
import cjs.DF_Plugin.pylon.item.PylonItemFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * 보스 몬스터 사망 관련 이벤트를 처리하는 리스너
 */
public class BossDeathListener implements Listener {

    private final DF_Main plugin;

    public BossDeathListener(DF_Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // 위더 처치 시
        if (event.getEntityType() == EntityType.WITHER) {
            // 기존 드롭 아이템(네더의 별)을 제거합니다.
            event.getDrops().removeIf(itemStack -> itemStack.getType() == Material.NETHER_STAR);
            // 보조 파일런 코어를 드롭합니다.
            event.getDrops().add(PylonItemFactory.createAuxiliaryCore());
            plugin.getLogger().info("위더가 처치되어 보조 파일런 코어를 드롭했습니다. (기존 네더의 별 드롭은 제거됨)");
        }

        // 엔더 드래곤 처치 시
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            // 기본 드롭(경험치)은 유지하고, 아이템 드롭은 여기서 제어
            event.getDrops().clear(); // 용의 알 등 기본 아이템 드롭 제거

            Player killer = event.getEntity().getKiller();
            World world = event.getEntity().getWorld();

            // 엔드 이벤트 매니저를 통해 보상 및 붕괴 시퀀스 시작
            plugin.getEndEventManager().triggerDragonDefeatSequence();

            // 킬러에게 마스터 컴퍼스 지급
            if (killer != null) {
                Location rewardCenter = new Location(world, 0, 200, 0);
                killer.getInventory().addItem(SpecialItemFactory.createMasterCompass(rewardCenter));
                killer.sendMessage("§d엔더 드래곤을 처치하여 마스터 컴퍼스를 획득했습니다!");
            }
        }
    }
}