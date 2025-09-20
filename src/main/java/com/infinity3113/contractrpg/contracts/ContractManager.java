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
        loadContractsFromFile("daily.yml", ContractType.DAILY);
        loadContractsFromFile("weekly.yml", ContractType.WEEKLY);
        loadContractsFromFile("special.yml", ContractType.SPECIAL);
    }

    private void loadContractsFromFile(String fileName, ContractType type) {
        File file = new File(plugin.getDataFolder() + "/contracts", fileName);
        if (!file.exists()) {
            plugin.saveResource("contracts/" + fileName, false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection contractsSection = config.getConfigurationSection("contracts");
        if (contractsSection == null) return;

        for (String id : contractsSection.getKeys(false)) {
            ConfigurationSection section = contractsSection.getConfigurationSection(id);
            if(section == null) continue;

            String displayName = section.getString("display-name");
            List<String> description = section.getStringList("description");
            
            MissionType missionType;
            try {
                missionType = MissionType.valueOf(section.getString("mission-type", "HUNTING").toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid mission-type in contract " + id + " in " + fileName);
                continue;
            }

            String missionObjective = section.getString("mission-objective");
            int missionRequirement = section.getInt("mission-requirement");
            List<String> rewards = section.getStringList("rewards");
            List<String> displayRewards = section.getStringList("display-rewards");
            int experienceReward = section.getInt("experience-reward", 0);
            int contractPointsReward = section.getInt("contract-points-reward", 0);
            int levelRequirement = section.getInt("level-requirement", 1);

            Contract contract = new Contract(id, displayName, description, type, missionType, missionObjective, missionRequirement, rewards, displayRewards, experienceReward, contractPointsReward, levelRequirement);
            contracts.put(id, contract);
        }
    }

    public String getFormattedMission(Contract contract) {
        if (contract == null) {
            return "Misión desconocida";
        }
        String format = plugin.getLangManager().getMessage("mission-formats." + contract.getMissionType().name());
        
        // ===== CORRECCIÓN AQUÍ =====
        // Se corrige la llamada a getMessage para que solo use un parámetro.
        String targetKey = "translation-keys." + contract.getMissionObjective();
        String translatedTarget = plugin.getLangManager().getMessage(targetKey); 
        // Si no hay traducción, LangManager devuelve la clave, así que comprobamos si son iguales.
        if(translatedTarget.equals(targetKey)) {
            translatedTarget = contract.getMissionObjective(); // Usamos el objetivo original si no hay traducción
        }

        return format
                .replace("%amount%", String.valueOf(contract.getMissionRequirement()))
                .replace("%target%", translatedTarget);
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
}