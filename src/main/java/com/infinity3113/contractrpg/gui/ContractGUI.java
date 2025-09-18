package com.infinity3113.contractrpg.gui;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.contracts.Contract;
import com.infinity3113.contractrpg.contracts.ContractManager;
import com.infinity3113.contractrpg.contracts.ContractType;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.util.ProgressBar;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContractGUI {

    private final ContractRPG plugin;
    private final Player player;
    private final ContractManager contractManager;

    public ContractGUI(ContractRPG plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.contractManager = plugin.getContractManager();
    }

    public void open() {
        Inventory gui = Bukkit.createInventory(null, 54, plugin.getLangManager().getMessage("gui-title"));
        
        // Populate GUI with items
        addContractItems(gui, ContractType.DAILY, 10);
        addContractItems(gui, ContractType.WEEKLY, 19);
        addContractItems(gui, ContractType.SPECIAL, 28);
        
        // Add decorative panes
        ItemStack grayPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = grayPane.getItemMeta();
        meta.setDisplayName(" ");
        grayPane.setItemMeta(meta);
        
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, grayPane);
            gui.setItem(i + 45, grayPane);
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
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', contract.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            for(String line : contract.getDescription()){
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            lore.add(" ");

            if (activeContracts.containsKey(contract.getId())) {
                lore.add(ChatColor.YELLOW + "Status: " + ChatColor.GOLD + "In Progress");
                int progress = playerData.getContractProgress(contract.getId());
                int total = contract.getMissionRequirement();
                lore.add(ProgressBar.create(progress, total, 20, "|", "&a", "&c"));
            } else {
                 lore.add(ChatColor.YELLOW + "Status: " + ChatColor.GREEN + "Available");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
            if((slot + 1) % 9 == 0) slot += 2; // Move to next row
        }
    }
}