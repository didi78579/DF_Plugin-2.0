package cjs.DF_Plugin.clan.storage;

import cjs.DF_Plugin.DF_Main;
import cjs.DF_Plugin.clan.Clan;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ClanStorageManager {
    private final DF_Main plugin;
    private final File clansFolder;
    private final File pylonStorageFolder;
    private final File giftBoxFolder;

    public ClanStorageManager(DF_Main plugin) {
        this.plugin = plugin;
        this.clansFolder = new File(plugin.getDataFolder(), "clans");
        if (!clansFolder.exists()) {
            clansFolder.mkdirs();
        }
        this.pylonStorageFolder = new File(plugin.getDataFolder(), "pylon_storage");
        if (!pylonStorageFolder.exists()) {
            pylonStorageFolder.mkdirs();
        }
        this.giftBoxFolder = new File(plugin.getDataFolder(), "gift_boxes");
        if (!giftBoxFolder.exists()) {
            giftBoxFolder.mkdirs();
        }
    }

    public Map<String, Clan> loadAllClans() {
        Map<String, Clan> clans = new HashMap<>();
        File[] clanFiles = clansFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (clanFiles == null) return clans;

        for (File clanFile : clanFiles) {
            try {
                FileConfiguration clanConfig = YamlConfiguration.loadConfiguration(clanFile);
                String clanName = clanFile.getName().replace(".yml", "");
                Clan clan = new Clan(clanName, clanConfig);
                clans.put(clanName.toLowerCase(), clan);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "손상되었거나 잘못된 클랜 파일을 불러오는데 실패했습니다: " + clanFile.getName(), e);
            }
        }
        return clans;
    }

    public void saveClan(Clan clan) {
        File clanFile = new File(clansFolder, clan.getName() + ".yml");
        FileConfiguration clanConfig = new YamlConfiguration();
        clan.save(clanConfig); // Clan의 save 메소드가 config를 채움
        try {
            clanConfig.save(clanFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save clan " + clan.getName(), e);
        }
    }

    public void deleteClan(String clanName) {
        File clanFile = new File(clansFolder, clanName + ".yml");
        if (clanFile.exists()) {
            if (!clanFile.delete()) {
                plugin.getLogger().severe("Failed to delete clan file for " + clanName);
            }
        }
    }
    public Inventory loadPylonStorage(Clan clan) {
        File storageFile = new File(pylonStorageFolder, clan.getName() + ".yml");
        Inventory inventory = Bukkit.createInventory(null, 54, clan.getDisplayName() + " §r§f파일런 창고"); // §r to reset color

        if (storageFile.exists()) {
            FileConfiguration storageConfig = YamlConfiguration.loadConfiguration(storageFile);
            if (storageConfig.contains("inventory.content")) {
                try {
                    List<?> rawList = storageConfig.getList("inventory.content");
                    if (rawList != null) {
                        ItemStack[] content = rawList.stream()
                                .map(item -> item instanceof ItemStack ? (ItemStack) item : null)
                                .toArray(ItemStack[]::new);
                        inventory.setContents(content);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to load pylon storage for clan " + clan.getName() + ": " + e.getMessage());
                }
            }
        }
        return inventory;
    }

    public void savePylonStorage(Clan clan, Inventory inventory) {
        File storageFile = new File(pylonStorageFolder, clan.getName() + ".yml");
        FileConfiguration storageConfig = new YamlConfiguration();
        storageConfig.set("inventory.content", inventory.getContents());
        try {
            storageConfig.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save pylon storage for clan " + clan.getName() + ": " + e.getMessage());
        }
    }

    public Inventory loadGiftBox(Clan clan) {
        File storageFile = new File(giftBoxFolder, clan.getName() + ".yml");
        Inventory inventory = Bukkit.createInventory(null, 27, "§d[" + clan.getDisplayName() + "§d] 선물상자");

        if (storageFile.exists()) {
            FileConfiguration storageConfig = YamlConfiguration.loadConfiguration(storageFile);
            if (storageConfig.contains("inventory.content")) {
                try {
                    List<?> rawList = storageConfig.getList("inventory.content");
                    if (rawList != null) {
                        ItemStack[] content = rawList.stream()
                                .map(item -> item instanceof ItemStack ? (ItemStack) item : null)
                                .toArray(ItemStack[]::new);
                        inventory.setContents(content);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to load gift box for clan " + clan.getName() + ": " + e.getMessage());
                }
            }
        }
        return inventory;
    }

    public void saveGiftBox(Clan clan, Inventory inventory) {
        File storageFile = new File(giftBoxFolder, clan.getName() + ".yml");
        FileConfiguration storageConfig = new YamlConfiguration();
        storageConfig.set("inventory.content", inventory.getContents());
        try {
            storageConfig.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save gift box for clan " + clan.getName() + ": " + e.getMessage());
        }
    }
}