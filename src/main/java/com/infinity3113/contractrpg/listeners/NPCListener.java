package com.infinity3113.contractrpg.listeners;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.contracts.Contract;
import com.infinity3113.contractrpg.contracts.MissionType;
import com.infinity3113.contractrpg.data.PlayerData;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.Material;
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
        int npcId = plugin.getConfig().getInt("delivery-npc-id");

        if (event.getNPC().getId() == npcId) {
            boolean deliveredMmoItem = tryDeliverMmoItem(player);
            if (deliveredMmoItem) return;

            boolean deliveredVanilla = tryDeliverVanillaItem(player);
            if (deliveredVanilla) return;

            // ===== CORRECCIÓN AQUÍ =====
            plugin.getGuiManager().openMainGUI(player);
        }
    }

    private boolean tryDeliverVanillaItem(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType().isAir()) {
            return false;
        }

        Material itemType = itemInHand.getType();
        PlayerData data = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (data == null) return false;

        for (String contractId : data.getActiveContracts().keySet()) {
            Contract contract = plugin.getContractManager().getContract(contractId);
            if (contract != null && contract.getMissionType() == MissionType.DELIVER) {
                try {
                    Material requiredType = Material.valueOf(contract.getMissionObjective().toUpperCase());
                    if (itemType == requiredType) {
                        int progress = data.getContractProgress(contractId) + itemInHand.getAmount();
                        int required = contract.getMissionRequirement();

                        if (progress >= required) {
                            player.getInventory().setItemInMainHand(null);
                            plugin.completeContract(player, contract);
                        } else {
                            data.setContractProgress(contractId, progress);
                            player.getInventory().setItemInMainHand(null);
                            player.sendMessage(plugin.getLangManager().getMessage("actionbar_progress")
                                    .replace("%mission%", contract.getDisplayName())
                                    .replace("%current%", String.valueOf(progress))
                                    .replace("%required%", String.valueOf(required)));
                        }
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    // Ignorar
                }
            }
        }
        return false;
    }

    private boolean tryDeliverMmoItem(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType().isAir()) {
            return false;
        }

        NBTItem nbtItem = NBTItem.get(itemInHand);
        if (!nbtItem.hasType()) {
            return false;
        }

        String mmoType = nbtItem.getType();
        String mmoId = nbtItem.getString("MMOITEMS_ITEM_ID");
        PlayerData data = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (data == null) return false;

        for (String contractId : data.getActiveContracts().keySet()) {
            Contract contract = plugin.getContractManager().getContract(contractId);
            if (contract != null && contract.getMissionType() == MissionType.DELIVER_MMOITEM) {
                String[] target = contract.getMissionObjective().split(":");
                if (target.length != 2) continue;
                String targetType = target[0];
                String targetId = target[1];

                if (mmoType.equalsIgnoreCase(targetType) && mmoId.equalsIgnoreCase(targetId)) {
                    int progress = data.getContractProgress(contractId) + itemInHand.getAmount();
                    int required = contract.getMissionRequirement();

                    if (progress >= required) {
                        player.getInventory().setItemInMainHand(null);
                        plugin.completeContract(player, contract);
                    } else {
                        data.setContractProgress(contractId, progress);
                        player.getInventory().setItemInMainHand(null);
                        player.sendMessage(plugin.getLangManager().getMessage("actionbar_progress")
                                .replace("%mission%", contract.getDisplayName())
                                .replace("%current%", String.valueOf(progress))
                                .replace("%required%", String.valueOf(required)));
                    }
                    return true;
                }
            }
        }
        return false;
    }
}