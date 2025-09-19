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
        File contractsFile = new File(plugin.getDataFolder(), "contracts.yml");
        if (!contractsFile.exists()) {
            plugin.saveResource("contracts.yml", false);
        }
        FileConfiguration contractsConfig = YamlConfiguration.loadConfiguration(contractsFile);

        ConfigurationSection contractsSection = contractsConfig.getConfigurationSection("contracts");
        if (contractsSection == null) {
            plugin.getLogger().warning("No 'contracts' section found in contracts.yml!");
            return;
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
                plugin.getLogger().severe("Failed to load contract '" + key + "'. Please check its configuration in contracts.yml.");
                plugin.getLogger().severe("Error details: " + e.getMessage());
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
    
    // MÉTODO AÑADIDO PARA SOLUCIONAR EL ERROR
    public String getFormattedMission(Contract contract) {
        String missionObjective = contract.getMissionObjective();
        String translatedObjective = plugin.getLangManager().getMessage(missionObjective);
        return translatedObjective.replace("%amount%", String.valueOf(contract.getMissionRequirement()));
    }
}