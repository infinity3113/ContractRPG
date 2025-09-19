package com.infinity3113.contractrpg.listeners;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.contracts.Contract;
import com.infinity3113.contractrpg.contracts.MissionType;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.util.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
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
                    // Ignorar
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
                   // Ignorar
                }
            }
        }
    }

    // --- ¡NUEVO EVENTO PARA PESCA! ---
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof Item)) return;

        Player player = event.getPlayer();
        PlayerData data = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (data == null) return;

        Material caughtType = ((Item) event.getCaught()).getItemStack().getType();

        for (String contractId : data.getActiveContracts().keySet()) {
            Contract contract = plugin.getContractManager().getContract(contractId);
            if (contract != null && contract.getMissionType() == MissionType.FISHING) {
                try {
                    Material requiredType = Material.valueOf(contract.getMissionObjective().toUpperCase());
                    if (caughtType == requiredType) {
                        updateContractProgress(player, data, contract);
                    }
                } catch (IllegalArgumentException e) {
                    // Ignorar
                }
            }
        }
    }

    // --- ¡NUEVO EVENTO PARA CRAFTEO! ---
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        PlayerData data = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (data == null) return;

        Material craftedType = event.getRecipe().getResult().getType();

        for (String contractId : data.getActiveContracts().keySet()) {
            Contract contract = plugin.getContractManager().getContract(contractId);
            if (contract != null && contract.getMissionType() == MissionType.CRAFTING) {
                try {
                    Material requiredType = Material.valueOf(contract.getMissionObjective().toUpperCase());
                    if (craftedType == requiredType) {
                        // El evento se llama por cada item crafteado, así que solo sumamos 1
                        updateContractProgress(player, data, contract);
                    }
                } catch (IllegalArgumentException e) {
                    // Ignorar
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