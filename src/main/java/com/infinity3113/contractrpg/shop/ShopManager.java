package com.infinity3113.contractrpg.shop;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.util.MessageUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopManager {

    private final ContractRPG plugin;
    private File shopFile;
    private FileConfiguration shopConfig;
    public final Map<UUID, String> adminEditing = new HashMap<>();

    public ShopManager(ContractRPG plugin) {
        this.plugin = plugin;
        this.shopFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }
        this.shopConfig = YamlConfiguration.loadConfiguration(shopFile);
    }

    public void saveItem(String id, ItemStack item, int price, int stockResetSeconds, int slot) {
        shopConfig.set("items." + id + ".item", ItemSerializer.serialize(item));
        shopConfig.set("items." + id + ".price", price);
        shopConfig.set("items." + id + ".stock-reset-seconds", stockResetSeconds);
        shopConfig.set("items." + id + ".slot", slot);
        saveConfig();
    }
    
    public void removeItem(String id) {
        shopConfig.set("items." + id, null);
        saveConfig();
    }

    public ConfigurationSection getItemsSection() {
        return shopConfig.getConfigurationSection("items");
    }

    public FileConfiguration getConfig() {
        return shopConfig;
    }

    public void saveConfig() {
        try {
            shopConfig.save(shopFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save shop.yml!");
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        this.shopConfig = YamlConfiguration.loadConfiguration(shopFile);
    }
}