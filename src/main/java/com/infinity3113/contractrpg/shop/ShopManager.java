package com.infinity3113.contractrpg.shop;

import com.infinity3113.contractrpg.ContractRPG;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ShopManager {

    private final ContractRPG plugin;
    private File configFile;
    private FileConfiguration config;
    private final Map<Integer, ShopItem> shopItems;

    public ShopManager(ContractRPG plugin) {
        this.plugin = plugin;
        this.shopItems = new HashMap<>();
        reloadConfig();
    }

    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "shop.yml");
        }
        if (!configFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadItems();
    }

    public void saveItems() {
        config.set("items", null); // Limpia la sección de items antiguos
        for (Map.Entry<Integer, ShopItem> entry : shopItems.entrySet()) {
            config.set("items." + entry.getKey(), ItemSerializer.serialize(entry.getValue()));
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar la tienda en shop.yml", e);
        }
    }

    private void loadItems() {
        shopItems.clear();
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                    if (itemSection != null) {
                        shopItems.put(slot, ItemSerializer.deserialize(itemSection));
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Clave de item inválida en shop.yml: " + key);
                }
            }
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public Map<Integer, ShopItem> getShopItems() {
        return shopItems;
    }

    public ShopItem getItem(int slot) {
        return shopItems.get(slot);
    }
    
    public void setItem(int slot, ShopItem item) {
        if (item == null) {
            shopItems.remove(slot);
        } else {
            shopItems.put(slot, item);
        }
        saveItems();
    }
}