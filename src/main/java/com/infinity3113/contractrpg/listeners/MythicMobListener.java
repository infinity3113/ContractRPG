package com.infinity3113.contractrpg.listeners;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.contracts.Contract;
import com.infinity3113.contractrpg.contracts.MissionType;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.util.MessageUtils;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MythicMobListener implements Listener {

    private final ContractRPG plugin;

    public MythicMobListener(ContractRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        if (!(event.getKiller() instanceof Player)) return;

        Player player = (Player) event.getKiller();
        PlayerData data = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (data == null) return;

        String mythicMobId = event.getMobType().getInternalName();

        for (String contractId : data.getActiveContracts().keySet()) {
            Contract contract = plugin.getContractManager().getContract(contractId);
            if (contract != null && contract.getMissionType() == MissionType.HUNT_MYTHICMOB) {
                if (contract.getMissionObjective().equalsIgnoreCase(mythicMobId)) {
                    updateContractProgress(player, data, contract);
                }
            }
        }
    }

    private void updateContractProgress(Player player, PlayerData data, Contract contract) {
        int currentProgress = data.getContractProgress(contract.getId());
        if (currentProgress >= contract.getMissionRequirement()) {
            return;
        }

        currentProgress++;
        data.setContractProgress(contract.getId(), currentProgress);

        String progressMessage = plugin.getLangManager().getMessage("actionbar_progress")
                .replace("%mission%", contract.getDisplayName())
                .replace("%current%", String.valueOf(currentProgress))
                .replace("%required%", String.valueOf(contract.getMissionRequirement()));
        MessageUtils.sendActionBar(player, progressMessage);

        if (currentProgress >= contract.getMissionRequirement()) {
            plugin.completeContract(player, contract);
        }
    }
}