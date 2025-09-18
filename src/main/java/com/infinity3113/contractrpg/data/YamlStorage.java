package com.infinity3113.contractrpg.data;

import com.infinity3113.contractrpg.ContractRPG;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class YamlStorage extends StorageManager {

    private final File dataFolder;

    public YamlStorage(ContractRPG plugin) {
        super(plugin);
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    @Override
    public void loadPlayerDataAsync(UUID uuid) {
        // CORRECCIÓN: Lectura de archivo asíncrona.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File playerFile = new File(dataFolder, uuid.toString() + ".yml");
            PlayerData playerData;
            if (playerFile.exists()) {
                FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                playerData = new PlayerData(uuid);
                playerData.setLevel(playerConfig.getInt("level", 1));
                playerData.setExperience(playerConfig.getInt("experience", 0));
                playerData.deserializeContracts(playerConfig.getString("contracts"));
            } else {
                playerData = new PlayerData(uuid);
            }

            final PlayerData finalPlayerData = playerData;
            // Volvemos al hilo principal para añadir a la caché de forma segura.
            Bukkit.getScheduler().runTask(plugin, () -> {
                addToCache(uuid, finalPlayerData);
            });
        });
    }

    @Override
    public void savePlayerDataAsync(PlayerData playerData) {
        // CORRECCIÓN: Escritura de archivo asíncrona.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            savePlayerDataSync(playerData);
        });
    }

    @Override
    public void savePlayerDataSync(PlayerData playerData) {
        File playerFile = new File(dataFolder, playerData.getUuid().toString() + ".yml");
        FileConfiguration playerConfig = new YamlConfiguration();

        playerConfig.set("level", playerData.getLevel());
        playerConfig.set("experience", playerData.getExperience());
        playerConfig.set("contracts", playerData.serializeContracts());

        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data for " + playerData.getUuid().toString());
            e.printStackTrace();
        }
    }
}