package com.infinity3113.contractrpg.gui;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.contracts.Contract;
import com.infinity3113.contractrpg.contracts.ContractManager;
import com.infinity3113.contractrpg.contracts.ContractType;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.util.MessageUtils;
import com.infinity3113.contractrpg.util.ProgressBar;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContractGUI {

    private final ContractRPG plugin;
    private final Player player;
    private final ContractManager contractManager;
    private final NamespacedKey contractIdKey;

    public ContractGUI(ContractRPG plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.contractManager = plugin.getContractManager();
        this.contractIdKey = new NamespacedKey(plugin, "contract-id");
    }

    public void open() {
        Inventory gui = Bukkit.createInventory(null, 54, MessageUtils.parse(plugin.getLangManager().getMessage("gui-title")));
        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData == null) return;
        
        ItemStack statsItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName(MessageUtils.parse(plugin.getLangManager().getMessage("gui-stats-item-name")));
        
        List<String> statsLore = new ArrayList<>();
        for(String line : plugin.getLangManager().getMessageList("gui-stats-item-lore")) {
            statsLore.add(MessageUtils.parse(line
                .replace("%level%", String.valueOf(playerData.getLevel()))
                .replace("%current_exp%", String.valueOf(playerData.getExperience()))
                .replace("%required_exp%", String.valueOf(playerData.getRequiredExperience()))
                .replace("%contract_points%", String.valueOf(playerData.getContractPoints()))
            ));
        }
        statsMeta.setLore(statsLore);
        statsItem.setItemMeta(statsMeta);
        gui.setItem(4, statsItem);

        addContractItems(gui, ContractType.DAILY, 10);
        addContractItems(gui, ContractType.WEEKLY, 19);
        addContractItems(gui, ContractType.SPECIAL, 28);
        
        ItemStack grayPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = grayPane.getItemMeta();
        meta.setDisplayName(" ");
        grayPane.setItemMeta(meta);
        
        for (int i : new int[]{0,1,2,3,5,6,7,8,45,46,47,48,49,50,51,52,53}) {
            gui.setItem(i, grayPane);
        }

        player.openInventory(gui);
    }

    private void addContractItems(Inventory gui, ContractType type, int startSlot) {
        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData == null) return;

        List<Contract> contracts = contractManager.getContractsByType(type);
        Map<String, Integer> activeContracts = playerData.getActiveContracts();

        int slot = startSlot;
        for (Contract contract : contracts) {
            ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(MessageUtils.parse(contract.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            for(String line : contract.getDescription()){
                lore.add(MessageUtils.parse(line));
            }
            lore.add(" ");

            if (activeContracts.containsKey(contract.getId())) {
                lore.add(MessageUtils.parse("<yellow>Status: <gold>En Progreso"));
                int progress = playerData.getContractProgress(contract.getId());
                int total = contract.getMissionRequirement();
                // CORRECCIÓN: Llamada correcta al método create
                lore.add(MessageUtils.parse(ProgressBar.create(progress, total, 20, "|", "<green>", "<red>")));
            } else {
                 lore.add(MessageUtils.parse("<yellow>Status: <green>Disponible"));
                 lore.add(" ");
                 lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("gui_click_to_accept")));
            }

            lore.add(" ");
            lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("gui-rewards-preview-header")));
            for(String rewardLine : contract.getDisplayRewards()){
                lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("gui-rewards-preview-format").replace("%reward%", rewardLine)));
            }
            if(contract.getExperienceReward() > 0){
                lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("gui-rewards-preview-format").replace("%reward%", contract.getExperienceReward() + " EXP")));
            }
            if(contract.getContractPointsReward() > 0){
                lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("gui-rewards-preview-format").replace("%reward%", contract.getContractPointsReward() + " Puntos de Contrato")));
            }

            meta.getPersistentDataContainer().set(contractIdKey, PersistentDataType.STRING, contract.getId());
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
            if((slot + 1) % 9 == 0) slot += 2;
        }
    }
}