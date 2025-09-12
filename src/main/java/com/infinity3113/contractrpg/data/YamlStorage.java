package com.example.contractrpg.data;

import com.example.contractrpg.ContractRPG;
import com.example.contractrpg.contracts.Contract;
import com.example.contractrpg.contracts.ContractManager;
import com.example.contractrpg.contracts.ContractType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class YamlStorage extends StorageManager {

    public YamlStorage(ContractRPG plugin, ContractManager contractManager) {
        super(plugin, contractManager);
    }

    @Override
    public void init() {
        plugin.getLogger().info("Usando almacenamiento de datos YAML.");
    }

    @Override
    public void shutdown() {
        // No se requiere acción.
    }

    @Override
    public void loadPlayerData(Player player) {
        File playerFile = getPlayerFile(player.getUniqueId());
        PlayerData playerData = new PlayerData(player.getUniqueId());

        if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            playerData.setNextDailyReset(config.getLong("next-daily-reset", 0));
            playerData.setNextWeeklyReset(config.getLong("next-weekly-reset", 0));

            config.getStringList("active-contracts").forEach(contractString -> {
                String[] parts = contractString.split(";", 3);
                if (parts.length == 3) {
                    ContractType type = ContractType.valueOf(parts[0]);
                    Contract contract = contractManager.parseContract(parts[1], type);
                    if (contract != null) {
                        contract.setCurrentAmount(Integer.parseInt(parts[2]));
                        playerData.addActiveContract(contract);
                    }
                }
            });
        }
        contractManager.loadPlayerDataIntoMap(player, playerData);
    }

    @Override
    public void savePlayerData(Player player) {
        PlayerData playerData = contractManager.getPlayerData(player);
        if (playerData == null) return;

        File playerFile = getPlayerFile(player.getUniqueId());
        FileConfiguration config = new YamlConfiguration();

        config.set("next-daily-reset", playerData.getNextDailyReset());
        config.set("next-weekly-reset", playerData.getNextWeeklyReset());

        List<String> activeContractsStrings = playerData.getActiveContracts().stream()
                .map(contract -> contract.getContractType().name() + ";" + contractToString(contract) + ";" + contract.getCurrentAmount())
                .collect(Collectors.toList());
        config.set("active-contracts", activeContractsStrings);

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar el archivo de datos para " + player.getName());
            e.printStackTrace();
        }
    }

    private File getPlayerFile(UUID uuid) {
        File dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        return new File(dataFolder, uuid.toString() + ".yml");
    }
}