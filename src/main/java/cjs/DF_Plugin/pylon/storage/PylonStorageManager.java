package cjs.DF_Plugin.pylon.storage;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import cjs.DF_Plugin.util.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PylonStorageManager {
    private final DF_Main plugin;
    private final File storageFile;
    private FileConfiguration storageConfig;
    private final Map<String, Inventory> clanStorages = new HashMap<>();

    public static final String STORAGE_TITLE = "§6[파일런 창고]";
    private static final String PYLON_OWNER_PATH = "pylons.%s.owner";
    private static final String PYLON_MEMBERS_PATH = "pylons.%s.members";
    private static final String PYLON_LOCATIONS_PATH = "pylons.%s.locations";
    private static final String PYLON_COLOR_PATH = "pylons.%s.color";
    private static final String STORAGE_PATH = "storages.%s";

    public PylonStorageManager(DF_Main plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "pylon_storages.yml");
        loadStorages();
    }

    public void saveClanPylons(Clan clan) {
        String clanName = clan.getName();
        storageConfig.set(String.format(PYLON_OWNER_PATH, clanName), clan.getLeader().toString());
        storageConfig.set(String.format(PYLON_COLOR_PATH, clanName), clan.getColor());

        List<String> memberUUIDs = new ArrayList<>();
        for (UUID memberId : clan.getMembers()) {
            memberUUIDs.add(memberId.toString());
        }
        storageConfig.set(String.format(PYLON_MEMBERS_PATH, clanName), memberUUIDs);

        List<String> serializedLocations = new ArrayList<>();
        for (String locStr : clan.getPylonLocations()) {
            serializedLocations.add(locStr);
        }
        storageConfig.set(String.format(PYLON_LOCATIONS_PATH, clanName), serializedLocations);

        saveConfig();
    }

    public void removeClanPylons(String clanName) {
        storageConfig.set("pylons." + clanName, null);
        storageConfig.set(String.format(STORAGE_PATH, clanName), null);
        saveConfig();
    }

    public void loadAllPylons() {
        ConfigurationSection pylonSection = storageConfig.getConfigurationSection("pylons");
        if (pylonSection == null) return;

        Set<String> clanNames = pylonSection.getKeys(false);
        for (String clanName : clanNames) {
            UUID ownerUUID = UUID.fromString(pylonSection.getString(clanName + ".owner"));
            String color = pylonSection.getString(clanName + ".color", "§f"); // 기본값 흰색
            Clan clan = plugin.getClanManager().getOrCreateClan(clanName, ownerUUID, color);

            List<String> memberUUIDs = pylonSection.getStringList(clanName + ".members");
            for (String uuidStr : memberUUIDs) {
                clan.addMember(UUID.fromString(uuidStr));
            }

            List<String> pylonLocations = pylonSection.getStringList(clanName + ".locations");
            for (String locStr : pylonLocations) {
                Location pylonLoc = PluginUtils.deserializeLocation(locStr);
                if (pylonLoc != null) {
                    clan.addPylonLocation(locStr);
                    plugin.getPylonManager().getAreaManager().addProtectedPylon(pylonLoc, clan);
                }
            }
            loadStorage(clan);
        }
    }

    private void loadStorage(Clan clan) {
        Inventory storage = Bukkit.createInventory(null, 54, STORAGE_TITLE);
        loadStorage(clan, storage);
        clanStorages.put(clan.getName(), storage);
    }

    /**
     * 지정된 인벤토리에 파일로부터 클랜 창고 데이터를 불러옵니다.
     * @param clan 대상 클랜
     * @param storage 아이템을 채울 인벤토리
     */
    private void loadStorage(Clan clan, Inventory storage) {
        ConfigurationSection storageSection = storageConfig.getConfigurationSection(String.format(STORAGE_PATH, clan.getName()));
        if (storageSection != null) {
            for (String key : storageSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = storageSection.getItemStack(key);
                    if (slot >= 0 && slot < storage.getSize()) {
                        storage.setItem(slot, item);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public void loadStorages() {
        if (!storageFile.exists()) {
            try {
                // 부모 디렉토리가 존재하지 않으면 생성
                storageFile.getParentFile().mkdirs();
                // 새로운 빈 파일 생성
                storageFile.createNewFile();
                plugin.getLogger().info("Created a new pylon_storages.yml file.");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create pylon_storages.yml!");
                e.printStackTrace();
            }
        }
        storageConfig = YamlConfiguration.loadConfiguration(storageFile);
    }

    public void openStorage(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§c소속된 가문이 없습니다.");
            return;
        }
        // 클랜 창고가 메모리에 없으면 생성하고 로드합니다.
        Inventory storage = clanStorages.computeIfAbsent(clan.getName(), k -> {
            Inventory newInv = Bukkit.createInventory(null, 54, STORAGE_TITLE);
            loadStorage(clan, newInv); // 파일에서 아이템을 로드하여 새 인벤토리에 채웁니다.
            return newInv;
        });
        player.openInventory(storage);
    }

    public void saveStorage(Clan clan) {
        Inventory storage = clanStorages.get(clan.getName());
        if (storage == null) return;

        String storagePath = String.format(STORAGE_PATH, clan.getName());
        storageConfig.set(storagePath, null); // 이전 데이터를 지웁니다.
        for (int i = 0; i < storage.getSize(); i++) {
            ItemStack item = storage.getItem(i);
            if (item != null) {
                storageConfig.set(storagePath + "." + i, item);
            }
        }
        saveConfig();
    }

    private void saveConfig() {
        try {
            storageConfig.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save pylon_storages.yml!");
            e.printStackTrace();
        }
    }
}