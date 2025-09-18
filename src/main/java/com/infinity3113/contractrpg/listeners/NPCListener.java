package com.infinity3113.contractrpg.listeners;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.contracts.Contract;
import com.infinity3113.contractrpg.contracts.MissionType;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.gui.ContractGUI;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class NPCListener implements Listener {

    private final ContractRPG plugin;

    public NPCListener(ContractRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNPCRightClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        // CORRECCIÓN: Se usa la clave correcta "delivery-npc-id" para que coincida con config.yml
        int npcId = plugin.getConfig().getInt("delivery-npc-id");

        if (event.getNPC().getId() == npcId) {
            // Intenta procesar una entrega de item. Si devuelve 'true', significa que se entregó algo.
            boolean delivered = tryDeliverMmoItem(player);

            // Si no se entregó ningún item, abre el menú como de costumbre.
            if (!delivered) {
                new ContractGUI(plugin, player).open();
            }
        }
    }

    private boolean tryDeliverMmoItem(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType().isAir()) {
            return false; // No tiene nada en la mano
        }

        NBTItem nbtItem = NBTItem.get(itemInHand);
        if (nbtItem == null || !nbtItem.hasType()) {
            return false; // No es un MMOItem válido
        }
        
        String mmoType = nbtItem.getType();
        String mmoId = nbtItem.getString("MMOITEMS_ITEM_ID");

        PlayerData data = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (data == null) return false;

        // Itera sobre los contratos activos del jugador
        for (String contractId : data.getActiveContracts().keySet()) {
            Contract contract = plugin.getContractManager().getContract(contractId);

            if (contract != null && contract.getMissionType() == MissionType.DELIVER_MMOITEM) {
                // El objetivo del contrato debe estar en formato "TIPO:ID", ej: "SWORD:CUTLASS"
                String[] target = contract.getMissionObjective().split(":");
                if (target.length != 2) continue; // Formato incorrecto en contracts.yml

                String targetType = target[0];
                String targetId = target[1];

                // Comprueba si el item en la mano es el que pide el contrato
                if (mmoType.equalsIgnoreCase(targetType) && mmoId.equalsIgnoreCase(targetId)) {
                    int progress = data.getContractProgress(contractId) + itemInHand.getAmount();
                    int required = contract.getMissionRequirement();

                    if (progress >= required) {
                        // Contrato completado
                        player.getInventory().setItemInMainHand(null); // Quita todos los items
                        player.sendMessage(plugin.getLangManager().getMessage("contract-completed").replace("%contract%", contract.getDisplayName()));
                        
                        // Dar recompensas
                        for (String rewardCommand : contract.getRewards()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewardCommand.replace("%player%", player.getName()));
                        }
                        data.removeContract(contractId);
                        
                    } else {
                        // Aún no se completa, solo actualiza el progreso
                        data.setContractProgress(contractId, progress);
                        player.getInventory().setItemInMainHand(null);
                        player.sendMessage(plugin.getLangManager().getMessage("contract-progress")
                            .replace("%contract%", contract.getDisplayName())
                            .replace("%progress%", String.valueOf(progress))
                            .replace("%total%", String.valueOf(required)));
                    }
                    return true; // Se procesó una entrega, detenemos el proceso.
                }
            }
        }
        return false;
    }
}