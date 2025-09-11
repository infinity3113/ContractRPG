package com.example.contractrpg.listeners;

import com.example.contractrpg.ContractRPG;
import com.example.contractrpg.contracts.Contract;
import com.example.contractrpg.contracts.ContractManager;
import com.example.contractrpg.contracts.MissionType;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type; // <-- IMPORTACIÓN NECESARIA
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class NPCListener implements Listener {

    private final ContractRPG plugin;
    private final ContractManager contractManager;

    public NPCListener(ContractRPG plugin) {
        this.plugin = plugin;
        this.contractManager = plugin.getContractManager();
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        int clickedNpcId = event.getNPC().getId();
        int deliveryNpcId = plugin.getConfig().getInt("delivery-npc-id", -1);

        if (clickedNpcId != deliveryNpcId) {
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType().isAir()) {
            plugin.getLangManager().sendMessage(player, "npc_delivery_greet");
            return;
        }

        NBTItem nbtItem = NBTItem.get(itemInHand);
        String mmoitemId = MMOItems.getID(nbtItem);
        
        // --- LÍNEA CORREGIDA ---
        // Obtenemos el objeto Type, no un String directamente.
        Type mmoitemTypeObject = MMOItems.getType(nbtItem);

        if (mmoitemId == null || mmoitemTypeObject == null) {
            plugin.getLangManager().sendMessage(player, "npc_delivery_not_mmoitem");
            return;
        }

        // --- LÍNEA CORREGIDA ---
        // Usamos .getId() para obtener el texto del tipo y construir el identificador.
        String itemIdentifier = mmoitemTypeObject.getId() + ":" + mmoitemId;
        boolean contractFound = false;

        for (Contract contract : contractManager.getPlayerData(player).getActiveContracts()) {
            if (contract.isCompleted() || contract.getMissionType() != MissionType.DELIVER_MMOITEM) {
                continue;
            }

            if (contract.getTarget().equalsIgnoreCase(itemIdentifier)) {
                contractFound = true;
                int amountInHand = itemInHand.getAmount();
                int amountNeeded = contract.getRequiredAmount() - contract.getCurrentAmount();
                int amountToTake = Math.min(amountInHand, amountNeeded);

                itemInHand.setAmount(amountInHand - amountToTake);
                
                contract.setCurrentAmount(contract.getCurrentAmount() + amountToTake);
                contractManager.updatePlayerProgress(player, contract);
                break;
            }
        }

        if (!contractFound) {
            plugin.getLangManager().sendMessage(player, "npc_delivery_wrong_item");
        }
    }
}