package com.infinity3113.contractrpg.data;

import com.infinity3113.contractrpg.ContractRPG;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StorageManager {

    protected final ContractRPG plugin;
    // Usamos ConcurrentHashMap para la caché, ya que se accederá desde hilos síncronos y asíncronos.
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();

    public StorageManager(ContractRPG plugin) {
        this.plugin = plugin;
    }

    // CORRECCIÓN: Métodos abstractos para cargar y guardar datos.
    // Las implementaciones (YAML/SQLite) se encargarán de la lógica asíncrona.
    public abstract void loadPlayerDataAsync(UUID uuid);
    public abstract void savePlayerDataAsync(PlayerData playerData);

    // --- Lógica de la Caché ---

    public void loadPlayerData(UUID uuid) {
        loadPlayerDataAsync(uuid);
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataCache.remove(uuid);
        if (data != null) {
            savePlayerDataAsync(data);
        }
    }

    public PlayerData getPlayerDataFromCache(UUID uuid) {
        return playerDataCache.get(uuid);
    }

    public void addToCache(UUID uuid, PlayerData data) {
        playerDataCache.put(uuid, data);
    }

    public void onDisable() {
        plugin.getLogger().info("Saving data for all online players...");
        for (UUID uuid : playerDataCache.keySet()) {
            PlayerData data = playerDataCache.get(uuid);
            if (data != null) {
                // En el apagado, es mejor guardar de forma síncrona para asegurar que se complete.
                savePlayerDataSync(data);
            }
        }
        playerDataCache.clear();
        plugin.getLogger().info("All player data saved.");
    }

    // Método de guardado síncrono para usar solo en onDisable
    public abstract void savePlayerDataSync(PlayerData playerData);
}