package com.infinity3113.contractrpg.shop;

import com.infinity3113.contractrpg.ContractRPG;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ShopManager {

    private final ContractRPG plugin;
    private final File shopConfigFile;
    private FileConfiguration shopConfig;

    // Usamos un mapa concurrente para seguridad entre hilos y un Lock para operaciones de archivo.
    private final Map<Integer, ShopItem> shopItems = new ConcurrentHashMap<>();
    private final ReentrantLock fileLock = new ReentrantLock();

    public ShopManager(ContractRPG plugin) {
        this.plugin = plugin;
        this.shopConfigFile = new File(plugin.getDataFolder(), "shop.yml");
        loadFromFile();
    }

    /**
     * Carga todos los ítems desde el archivo shop.yml a la memoria.
     * Esta operación es segura contra hilos.
     */
    public void loadFromFile() {
        fileLock.lock();
        try {
            if (!shopConfigFile.exists()) {
                plugin.saveResource("shop.yml", false);
            }
            shopConfig = YamlConfiguration.loadConfiguration(shopConfigFile);
            shopItems.clear();
            ConfigurationSection itemsSection = shopConfig.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(key);
                        ShopItem item = (ShopItem) itemsSection.get(key);
                        if (item != null) {
                            shopItems.put(slot, item);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error al cargar el ítem de la tienda con clave: " + key);
                    }
                }
            }
        } finally {
            fileLock.unlock();
        }
    }

    /**
     * Guarda todos los ítems de la memoria al archivo shop.yml.
     * Esta operación es segura contra hilos y garantiza la escritura.
     */
    private void saveToFile() {
        fileLock.lock();
        try {
            // Borra la sección antigua para asegurar que no queden ítems eliminados.
            shopConfig.set("items", null);
            for (Map.Entry<Integer, ShopItem> entry : shopItems.entrySet()) {
                shopConfig.set("items." + entry.getKey(), entry.getValue());
            }
            shopConfig.save(shopConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("¡No se pudo guardar la tienda en shop.yml!");
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    /**
     * Añade o actualiza un ítem y guarda inmediatamente los cambios.
     * @param slot El slot del ítem.
     * @param item El ShopItem a guardar.
     */
    public void setItemAndSave(int slot, ShopItem item) {
        shopItems.put(slot, item);
        saveToFile();
    }

    /**
     * Elimina un ítem y guarda inmediatamente los cambios.
     * @param slot El slot del ítem a eliminar.
     */
    public void removeItemAndSave(int slot) {
        shopItems.remove(slot);
        saveToFile();
    }
    
    /**
     * Encuentra el primer slot disponible para un nuevo ítem.
     * @return El ID del primer slot vacío.
     */
    public int findNextFreeSlot() {
        int slot = 0;
        while (shopItems.containsKey(slot)) {
            slot++;
        }
        return slot;
    }

    public ShopItem getItem(int slot) {
        return shopItems.get(slot);
    }

    public Map<Integer, ShopItem> getShopItems() {
        return Collections.unmodifiableMap(shopItems);
    }
    
    public FileConfiguration getConfig() {
        return shopConfig;
    }
}