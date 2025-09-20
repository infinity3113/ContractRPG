package com.infinity3113.contractrpg.managers;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.contracts.Contract;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.util.ProgressBar;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderManager extends PlaceholderExpansion {

    private final ContractRPG plugin;

    public PlaceholderManager(ContractRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "contractrpg";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public List<String> getPlaceholders(){
        List<String> placeholders = new ArrayList<>();
        placeholders.add("%contractrpg_level%");
        placeholders.add("%contractrpg_exp%");
        placeholders.add("%contractrpg_exp_required%");
        placeholders.add("%contractrpg_exp_bar%");
        placeholders.add("%contractrpg_active_contracts_count%");
        placeholders.add("%contractrpg_active_<1-N>_name%");
        placeholders.add("%contractrpg_active_<1-N>_objective%");
        placeholders.add("%contractrpg_active_<1-N>_progress%");
        placeholders.add("%contractrpg_active_<1-N>_required%");
        placeholders.add("%contractrpg_active_<1-N>_target%");
        return placeholders;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        PlayerData data = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (data == null) {
            return "Loading...";
        }

        if (params.equalsIgnoreCase("level")) return String.valueOf(data.getLevel());
        if (params.equalsIgnoreCase("exp")) return String.valueOf(data.getExperience());
        if (params.equalsIgnoreCase("exp_required")) return String.valueOf(data.getRequiredExperience());
        if (params.equalsIgnoreCase("exp_bar")) {
            // CORRECCIÓN: Llamada correcta al método create
            return ProgressBar.create(data.getExperience(), data.getRequiredExperience(), 20, "|", "<green>", "<gray>");
        }

        if (params.equalsIgnoreCase("active_contracts_count")) return String.valueOf(data.getActiveContracts().size());

        if (params.startsWith("active_")) {
            try {
                String[] parts = params.split("_");
                if(parts.length < 3) return null;
                
                int index = Integer.parseInt(parts[1]) - 1;
                String type = parts[2];

                if (index >= 0 && index < data.getActiveContracts().size()) {
                    String contractId = new ArrayList<>(data.getActiveContracts().keySet()).get(index);
                    Contract contract = plugin.getContractManager().getContract(contractId);
                    if (contract == null) return "Invalid Contract";

                    switch (type) {
                        case "name":
                            return contract.getDisplayName();
                        case "objective":
                            return plugin.getContractManager().getFormattedMission(contract);
                        case "progress":
                            return String.valueOf(data.getContractProgress(contractId));
                        case "required":
                            return String.valueOf(contract.getMissionRequirement());
                        case "target":
                            String missionObjective = contract.getMissionObjective();
                            return plugin.getLangManager().getTranslation(missionObjective);
                    }
                } else {
                    return "";
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }
        return null;
    }
}