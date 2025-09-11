package com.example.contractrpg.listeners;

import com.example.contractrpg.ContractRPG;
import com.example.contractrpg.contracts.Contract;
import com.example.contractrpg.contracts.ContractManager;
import com.example.contractrpg.contracts.MissionType;
import com.example.contractrpg.data.StorageManager;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final ContractManager contractManager;
    private final StorageManager storageManager;

    public PlayerListener(ContractRPG plugin) {
        this.contractManager = plugin.getContractManager();
        this.storageManager = plugin.getStorageManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        storageManager.loadPlayerData(player);
        contractManager.offerDailyContracts(player);
        contractManager.offerWeeklyContracts(player); // Llamada para misiones semanales
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        storageManager.savePlayerData(player);
        contractManager.removePlayerDataFromMap(player);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) return;
        Player killer = event.getEntity().getKiller();

        for (Contract contract : contractManager.getPlayerData(killer).getActiveContracts()) {
            if (contract.isCompleted()) continue;

            boolean progressMade = false;

            if (contract.getMissionType() == MissionType.KILL) {
                if (event.getEntity().getType().name().equalsIgnoreCase(contract.getTarget())) {
                    contract.setCurrentAmount(contract.getCurrentAmount() + 1);
                    progressMade = true;
                }
            }

            if (contract.getMissionType() == MissionType.KILL_MYTHIC) {
                if (MythicBukkit.inst().getMobManager().isMythicMob(event.getEntity())) {
                    String mythicMobId = MythicBukkit.inst().getMobManager().getMythicMobInstance(event.getEntity()).getType().getInternalName();
                    if (mythicMobId.equalsIgnoreCase(contract.getTarget())) {
                        contract.setCurrentAmount(contract.getCurrentAmount() + 1);
                        progressMade = true;
                    }
                }
            }

            if (progressMade) {
                contractManager.updatePlayerProgress(killer, contract);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        for (Contract contract : contractManager.getPlayerData(player).getActiveContracts()) {
            if (contract.isCompleted() || contract.getMissionType() != MissionType.BREAK) continue;
            
            if (event.getBlock().getType().name().equalsIgnoreCase(contract.getTarget())) {
                contract.setCurrentAmount(contract.getCurrentAmount() + 1);
                contractManager.updatePlayerProgress(player, contract);
            }
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        
        for (Contract contract : contractManager.getPlayerData(player).getActiveContracts()) {
            if (contract.isCompleted() || contract.getMissionType() != MissionType.FARM) continue;
            
            if (event.getItem().getItemStack().getType().name().equalsIgnoreCase(contract.getTarget())) {
                contract.setCurrentAmount(contract.getCurrentAmount() + event.getItem().getItemStack().getAmount());
                contractManager.updatePlayerProgress(player, contract);
            }
        }
    }
}