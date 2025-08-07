package cjs.DF_Plugin.upgrade.profile;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.upgrade.profile.type.*;
import org.bukkit.Material;

import java.util.*;
import java.util.stream.Collectors;

public class WeaponProfileManager {
    private final DF_Main plugin;
    private final Map<Material, IWeaponProfile> profiles = new HashMap<>();
    private final Map<String, List<Material>> itemTypeMap = new HashMap<>();

    public WeaponProfileManager(DF_Main plugin) {
        this.plugin = plugin;
        loadProfiles();
    }

    private void loadProfiles() {
        categorizeItems();

        // 각 장비 유형에 맞는 프로필 클래스를 인스턴스화하여 매핑합니다.
        mapProfileToType("sword", new SwordProfile());
        mapProfileToType("pickaxe", new PickaxeProfile());
        mapProfileToType("axe", new AxeProfile());
        mapProfileToType("shovel", new ShovelProfile());
        mapProfileToType("hoe", new HoeProfile());
        mapProfileToType("helmet", new HelmetProfile());
        mapProfileToType("chestplate", new ChestplateProfile());
        mapProfileToType("leggings", new LeggingsProfile());
        mapProfileToType("boots", new BootsProfile());
        mapProfileToType("shield", new ShieldProfile());
        mapProfileToType("fishing_rod", new FishingRodProfile());
        mapProfileToType("bow", new BowProfile());
        mapProfileToType("crossbow", new CrossbowProfile());
        mapProfileToType("trident", new TridentProfile());
    }

    private void mapProfileToType(String itemType, IWeaponProfile profile) {
        List<Material> materials = itemTypeMap.get(itemType);
        if (materials != null) {
            materials.forEach(material -> profiles.put(material, profile));
        }
    }

    private void categorizeItems() {
        itemTypeMap.put("sword", filterMaterials("_SWORD"));
        itemTypeMap.put("pickaxe", filterMaterials("_PICKAXE"));
        itemTypeMap.put("axe", filterMaterials("_AXE"));
        itemTypeMap.put("shovel", filterMaterials("_SHOVEL"));
        itemTypeMap.put("hoe", filterMaterials("_HOE"));
        itemTypeMap.put("helmet", filterMaterials("_HELMET"));
        itemTypeMap.put("chestplate", filterMaterials("_CHESTPLATE"));
        itemTypeMap.put("leggings", filterMaterials("_LEGGINGS"));
        itemTypeMap.put("boots", filterMaterials("_BOOTS"));
        itemTypeMap.put("shield", List.of(Material.SHIELD));
        itemTypeMap.put("fishing_rod", List.of(Material.FISHING_ROD));
        itemTypeMap.put("bow", List.of(Material.BOW));
        itemTypeMap.put("crossbow", List.of(Material.CROSSBOW));
        itemTypeMap.put("trident", List.of(Material.TRIDENT));
    }

    private List<Material> filterMaterials(String suffix) {
        return Arrays.stream(Material.values())
                .filter(m -> m.isItem() && !m.isLegacy() && m.name().endsWith(suffix))
                .collect(Collectors.toList());
    }

    public IWeaponProfile getProfile(Material material) {
        return profiles.get(material);
    }
}