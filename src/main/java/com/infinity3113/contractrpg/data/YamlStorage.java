package com.example.contractrpg.data;

import com.example.contractrpg.ContractRPG;
import com.example.contractrpg.contracts.Contract;
import com.example.contractrpg.contracts.ContractManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class YamlStorage implements StorageManager {

    private final ContractRPG plugin;
    private final ContractManager contractManager;
    private File dataFolder;

    public YamlStorage(ContractRPG plugin, ContractManager contractManager) {
        this.plugin = plugin;
        this.contractManager = contractManager;
    }

    @Override
    public void init() {
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    @Override
    public void loadPlayerData(Player player) {
        File playerFile = new File(dataFolder, player.getUniqueId() + ".yml");
        File backupFile = new File(dataFolder, player.getUniqueId() + ".yml.bak");
        PlayerData data = new PlayerData(player.getUniqueId());

        FileConfiguration config = null;
        if (playerFile.exists()) {
            config = YamlConfiguration.loadConfiguration(playerFile);
        } else if (backupFile.exists()) {
            plugin.getLogger().warning("No se encontró el archivo de datos para " + player.getName() + ", cargando desde el backup.");
            config = YamlConfiguration.loadConfiguration(backupFile);
        }

        if (config != null) {
            data.setNextDailyReset(config.getLong("next-daily-reset", 0));
            data.setNextWeeklyReset(config.getLong("next-weekly-reset", 0));

            List<Map<?, ?>> activeContractsList = config.getMapList("active-contracts");
            for (Map<?, ?> contractMap : activeContractsList) {
                String contractString = (String) contractMap.get("contract-string");
                int progress = (int) contractMap.get("progress");
                Contract contract = contractManager.parseContract(contractString);
                if (contract != null) {
                    contract.setCurrentAmount(progress);
                    data.getActiveContracts().add(contract);
                }
            }
        }
        contractManager.loadPlayerDataIntoMap(player, data);
    }

    @Override
    public void savePlayerData(Player player) {
        PlayerData data = contractManager.getPlayerData(player);
        if (data == null) return;

        File playerFile = new File(dataFolder, player.getUniqueId() + ".yml");
        File tempFile = new File(dataFolder, player.getUniqueId() + ".yml.tmp");
        File backupFile = new File(dataFolder, player.getUniqueId() + ".yml.bak");

        FileConfiguration config = new YamlConfiguration();
        config.set("next-daily-reset", data.getNextDailyReset());
        config.set("next-weekly-reset", data.getNextWeeklyReset());

        List<Map<String, Object>> activeContractsToSave = new ArrayList<>();
        for (Contract contract : data.getActiveContracts()) {
            Map<String, Object> contractData = new HashMap<>();
            String contractString = String.format(Locale.US, "%s:%s:%d:%f",
                    contract.getMissionType().name(), contract.getTarget(),
                    contract.getRequiredAmount(), contract.getReward());
            contractData.put("contract-string", contractString);
            contractData.put("progress", contract.getCurrentAmount());
            activeContractsToSave.add(contractData);
        }
        config.set("active-contracts", activeContractsToSave);

        try {
            config.save(tempFile);
            if (playerFile.exists()) {
                if (backupFile.exists()) backupFile.delete();
                playerFile.renameTo(backupFile);
            }
            tempFile.renameTo(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        // No es necesario para YAML
    }
}