package cjs.DF_Plugin.items;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

/**
 * 특별한 기능이 있는 아이템을 생성하는 팩토리 클래스입니다.
 */
public class SpecialItemFactory {

    /**
     * 드래곤 보상 지역을 가리키는 마스터 컴퍼스를 생성합니다.
     * @param target 보상 지역의 중심 좌표
     * @return 생성된 마스터 컴퍼스 아이템
     */
    public static ItemStack createMasterCompass(Location target) {
        ItemStack compass = new ItemBuilder(Material.COMPASS)
                .withName("§b§l마스터 컴퍼스")
                .withLore("§7드래곤의 보물이 흩뿌려진 곳을 가리킵니다.")
                .build();

        compass.editMeta(CompassMeta.class, meta -> {
            meta.setLodestone(target);
            meta.setLodestoneTracked(false); // 로드스톤 블록 없이 좌표를 추적합니다.
        });
        return compass;
    }
}