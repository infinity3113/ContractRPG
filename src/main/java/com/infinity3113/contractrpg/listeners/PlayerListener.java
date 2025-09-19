package com.infinity3113.contractrpg.listeners;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.contracts.Contract;
import com.infinity3113.contractrpg.contracts.ContractType;
import com.infinity3113.contractrpg.contracts.MissionType;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final ContractRPG plugin;

    public PlayerListener(ContractRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getStorageManager().loadPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getStorageManager().savePlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player player = event.getEntity().getKiller();
        PlayerData data = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (data == null) return;

        EntityType killedType = event.getEntityType();

        for (String contractId : data.getActiveContracts().keySet()) {
            Contract contract = plugin.getContractManager().getContract(contractId);
            if (contract != null && contract.getMissionType() == MissionType.HUNTING) {
                try {
                    EntityType requiredType = EntityType.valueOf(contract.getMissionObjective().toUpperCase());
                    if (killedType == requiredType) {
                        updateContractProgress(player, data, contract);
                    }
                } catch (IllegalArgumentException e) {
                    // Ignorar si el objetivo no es un tipo de entidad válido
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (data == null) return;

        Material brokenType = event.getBlock().getType();

        for (String contractId : data.getActiveContracts().keySet()) {
            Contract contract = plugin.getContractManager().getContract(contractId);
            if (contract != null && contract.getMissionType() == MissionType.MINING) {
                try {
                    Material requiredType = Material.valueOf(contract.getMissionObjective().toUpperCase());
                    if (brokenType == requiredType) {
                        updateContractProgress(player, data, contract);
                    }
                } catch (IllegalArgumentException e) {
                   // Ignorar si el objetivo no es un material válido
                }
            }
        }
    }

    private void updateContractProgress(Player player, PlayerData data, Contract contract) {
        int currentProgress = data.getContractProgress(contract.getId());
        if (currentProgress >= contract.getMissionRequirement()) {
            return; // Ya está completado, no hacer nada
        }

        currentProgress++;
        data.setContractProgress(contract.getId(), currentProgress);

        String progressMessage = plugin.getLangManager().getMessage("actionbar_progress")
                .replace("%mission%", contract.getDisplayName())
                .replace("%current%", String.valueOf(currentProgress))
                .replace("%required%", String.valueOf(contract.getMissionRequirement()));
        MessageUtils.sendActionBar(player, progressMessage);

        if (currentProgress >= contract.getMissionRequirement()) {
            MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("contract_completed"));
            for (String rewardCommand : contract.getRewards()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewardCommand.replace("%player%", player.getName()));
            }

            // Registrar como completado y luego remover
            if (contract.getContractType() == ContractType.DAILY) {
                data.addCompletedDailyContract(contract.getId());
            } else if (contract.getContractType() == ContractType.WEEKLY) {
                data.addCompletedWeeklyContract(contract.getId());
            }
            
            data.removeContract(contract.getId());
        }
    }
}