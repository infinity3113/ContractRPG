package com.infinity3113.contractrpg.contracts;

import com.infinity3113.contractrpg.ContractRPG;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContractManager {

    private final ContractRPG plugin;
    private final Map<String, Contract> contracts = new HashMap<>();

    public ContractManager(ContractRPG plugin) {
        this.plugin = plugin;
        loadContracts();
    }

    public void loadContracts() {
        contracts.clear();
        
        // Define la carpeta donde se almacenarán los archivos de contratos.
        File contractsFolder = new File(plugin.getDataFolder(), "contracts");

        // Si la carpeta no existe, la crea.
        if (!contractsFolder.exists()) {
            contractsFolder.mkdirs();
        }
        
        // Guarda los archivos de ejemplo desde el JAR si no existen en la carpeta de destino.
        saveDefaultContractFiles(contractsFolder);

        // Si la carpeta está vacía o no contiene archivos .yml, no hay nada que cargar.
        File[] contractFiles = contractsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (contractFiles == null || contractFiles.length == 0) {
            plugin.getLogger().info("No contract files found in 'contracts' folder. No contracts were loaded.");
            return;
        }
        
        // Itera sobre cada archivo .yml encontrado en la carpeta.
        for (File contractFile : contractFiles) {
            FileConfiguration contractConfig = YamlConfiguration.loadConfiguration(contractFile);
            ConfigurationSection contractsSection = contractConfig.getConfigurationSection("contracts");

            if (contractsSection == null) {
                plugin.getLogger().warning("No 'contracts' section found in " + contractFile.getName() + "!");
                continue;
            }

            for (String key : contractsSection.getKeys(false)) {
                try {
                    ConfigurationSection section = contractsSection.getConfigurationSection(key);
                    String displayName = section.getString("display-name");
                    List<String> description = section.getStringList("description");
                    ContractType contractType = ContractType.valueOf(section.getString("contract-type").toUpperCase());
                    MissionType missionType = MissionType.valueOf(section.getString("mission-type").toUpperCase());
                    String missionObjective = section.getString("mission-objective");
                    int missionRequirement = section.getInt("mission-requirement");
                    List<String> rewards = section.getStringList("rewards");
                    List<String> displayRewards = section.getStringList("display-rewards");
                    int experienceReward = section.getInt("experience-reward", 0);
                    int levelRequirement = section.getInt("level-requirement", 0);

                    Contract contract = new Contract(key, displayName, description, contractType, missionType, missionObjective, missionRequirement, rewards, displayRewards, experienceReward, levelRequirement);
                    contracts.put(key, contract);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to load contract '" + key + "' from " + contractFile.getName() + ". Please check its configuration.");
                    plugin.getLogger().severe("Error details: " + e.getMessage());
                }
            }
        }
    }

    private void saveDefaultContractFiles(File contractsFolder) {
        String[] defaultFiles = {"daily.yml", "weekly.yml", "special.yml"};
        for (String fileName : defaultFiles) {
            File destinationFile = new File(contractsFolder, fileName);
            // El archivo se guarda desde la carpeta 'contracts' dentro del JAR.
            if (!destinationFile.exists()) {
                plugin.saveResource("contracts/" + fileName, false);
            }
        }
    }

    public Contract getContract(String id) {
        return contracts.get(id);
    }

    public List<Contract> getContractsByType(ContractType type) {
        List<Contract> result = new ArrayList<>();
        for (Contract contract : contracts.values()) {
            if (contract.getContractType() == type) {
                result.add(contract);
            }
        }
        return result;
    }

    public String getFormattedMission(Contract contract) {
        String missionObjective = contract.getMissionObjective();
        String translatedObjective = plugin.getLangManager().getMessage(missionObjective);
        return translatedObjective.replace("%amount%", String.valueOf(contract.getMissionRequirement()));
    }
}