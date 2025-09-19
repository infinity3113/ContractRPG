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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File playerFile = new File(dataFolder, uuid.toString() + ".yml");
            PlayerData playerData;
            if (playerFile.exists()) {
                FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                playerData = new PlayerData(uuid);
                playerData.setLevel(playerConfig.getInt("level", 1));
                playerData.setExperience(playerConfig.getInt("experience", 0));
                
                // Cargar datos de contratos activos y completados
                playerData.deserializeActiveContracts(playerConfig.getString("active-contracts"));
                playerData.deserializeCompletedDaily(playerConfig.getString("completed-daily"));
                playerData.deserializeCompletedWeekly(playerConfig.getString("completed-weekly"));

            } else {
                playerData = new PlayerData(uuid);
            }

            final PlayerData finalPlayerData = playerData;
            Bukkit.getScheduler().runTask(plugin, () -> {
                addToCache(uuid, finalPlayerData);
            });
        });
    }

    @Override
    public void savePlayerDataAsync(PlayerData playerData) {
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
        
        // Guardar datos de contratos activos y completados
        playerConfig.set("active-contracts", playerData.serializeActiveContracts());
        playerConfig.set("completed-daily", playerData.serializeCompletedDaily());
        playerConfig.set("completed-weekly", playerData.serializeCompletedWeekly());

        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data for " + playerData.getUuid().toString());
            e.printStackTrace();
        }
    }
}