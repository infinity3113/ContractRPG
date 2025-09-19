package com.infinity3113.contractrpg.managers;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.contracts.Contract;
import com.infinity3113.contractrpg.contracts.ContractType;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.util.MessageUtils;
import com.infinity3113.contractrpg.util.ProgressBar;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GUIManager {

    private final ContractRPG plugin;
    public final NamespacedKey contractIdKey;
    public final NamespacedKey guiActionKey;
    private final ItemStack borderPane;
    private final ItemStack fillerPane;

    public GUIManager(ContractRPG plugin) {
        this.plugin = plugin;
        this.contractIdKey = new NamespacedKey(plugin, "contract-id");
        this.guiActionKey = new NamespacedKey(plugin, "gui-action");
        
        this.borderPane = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        this.fillerPane = createPane(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    public void openMainMenu(Player player) {
        String title = MessageUtils.parse(plugin.getLangManager().getMessage("gui.main.title"));
        Inventory gui = Bukkit.createInventory(null, 45, title);

        fillBorders(gui, borderPane);

        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData != null) {
            ItemStack statsItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
            ItemMeta statsMeta = statsItem.getItemMeta();
            statsMeta.setDisplayName(MessageUtils.parse(plugin.getLangManager().getMessage("gui.main.stats.name")));

            List<String> statsLore = new ArrayList<>();
            for (String line : plugin.getLangManager().getMessageList("gui.main.stats.lore")) {
                statsLore.add(MessageUtils.parse(line
                        .replace("%level%", String.valueOf(playerData.getLevel()))
                        .replace("%current_exp%", String.valueOf(playerData.getExperience()))
                        .replace("%required_exp%", String.valueOf(playerData.getRequiredExperience()))
                        .replace("%exp_bar%", ProgressBar.create(
                                playerData.getExperience(), 
                                playerData.getRequiredExperience(), 
                                20, "⎜", "green", "gray")) // <-- CORREGIDO AQUÍ
                ));
            }
            statsMeta.setLore(statsLore);
            statsMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            statsItem.setItemMeta(statsMeta);
            gui.setItem(4, statsItem);
        }

        gui.setItem(20, createMenuItem(Material.WRITABLE_BOOK, "gui.main.daily", "open_daily"));
        gui.setItem(21, createMenuItem(Material.WRITABLE_BOOK, "gui.main.weekly", "open_weekly"));
        gui.setItem(22, createMenuItem(Material.WRITABLE_BOOK, "gui.main.special", "open_special"));
        gui.setItem(24, createMenuItem(Material.BOOK, "gui.main.active", "open_active"));

        player.openInventory(gui);
    }

    public void openContractList(Player player, ContractType type) {
        String titleKey = "gui.submenu.title." + type.name().toLowerCase();
        String title = MessageUtils.parse(plugin.getLangManager().getMessage(titleKey));
        Inventory gui = Bukkit.createInventory(null, 54, title);

        fillBorders(gui, fillerPane);
        addNavigationButtons(gui, type);

        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        
        List<Contract> potentialContracts = plugin.getContractManager().getContractsByType(type).stream()
                .filter(c -> {
                    boolean isActive = playerData.getActiveContracts().containsKey(c.getId());
                    boolean isCompleted;
                    if (type == ContractType.DAILY) {
                        isCompleted = playerData.getCompletedDailyContracts().contains(c.getId());
                    } else if (type == ContractType.WEEKLY) {
                        isCompleted = playerData.getCompletedWeeklyContracts().contains(c.getId());
                    } else {
                        isCompleted = false;
                    }
                    return !isActive && !isCompleted;
                })
                .collect(Collectors.toList());

        Collections.shuffle(potentialContracts);

        String configPath = type.name().toLowerCase() + "-missions.offered-amount";
        int offerLimit = plugin.getConfig().getInt(configPath, 4);

        List<Contract> contractsToDisplay = new ArrayList<>();
        for (Contract contract : potentialContracts) {
            if (contractsToDisplay.size() >= offerLimit) {
                break;
            }
            if (playerData.getLevel() >= contract.getLevelRequirement()) {
                contractsToDisplay.add(contract);
            }
        }
        
        int slot = 10;
        for (Contract contract : contractsToDisplay) {
            gui.setItem(slot, createContractItem(contract, playerData, false));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        player.openInventory(gui);
    }

    public void openActiveContracts(Player player) {
        String title = MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.active"));
        Inventory gui = Bukkit.createInventory(null, 54, title);
        
        fillBorders(gui, fillerPane);
        addNavigationButtons(gui, null);
        
        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        
        int slot = 10;
        for (String contractId : playerData.getActiveContracts().keySet()) {
            Contract contract = plugin.getContractManager().getContract(contractId);
            if (contract != null) {
                gui.setItem(slot, createContractItem(contract, playerData, true));
                slot++;
                if (slot % 9 == 8) slot += 2;
            }
        }
        
        player.openInventory(gui);
    }

    private ItemStack createMenuItem(Material material, String langPath, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.parse(plugin.getLangManager().getMessage(langPath + ".name")));
        
        List<String> loreLines = plugin.getLangManager().getMessageList(langPath + ".lore");
        meta.setLore(loreLines.stream().map(MessageUtils::parse).collect(Collectors.toList()));
        
        meta.getPersistentDataContainer().set(guiActionKey, PersistentDataType.STRING, action);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createContractItem(Contract contract, PlayerData data, boolean isActive) {
        ItemStack item = new ItemStack(isActive ? Material.BOOK : Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.parse(contract.getDisplayName()));

        List<String> lore = new ArrayList<>();
        contract.getDescription().forEach(line -> lore.add(MessageUtils.parse(line)));
        lore.add(" ");

        if (!contract.getDisplayRewards().isEmpty() || contract.getExperienceReward() > 0) {
            lore.add(MessageUtils.parse("<gold>Recompensas:"));
            for (String rewardLine : contract.getDisplayRewards()) {
                lore.add(MessageUtils.parse("<gray>- <white>" + rewardLine));
            }
            if (contract.getExperienceReward() > 0) {
                lore.add(MessageUtils.parse("<gray>- <#FFFF55>" + contract.getExperienceReward() + " EXP"));
            }
            lore.add(" ");
        }

        if (isActive) {
            int progress = data.getContractProgress(contract.getId());
            int total = contract.getMissionRequirement();
            lore.add(MessageUtils.parse(ProgressBar.create(progress, total, 20, "⎜", "green", "gray"))); // <-- CORREGIDO AQUÍ
        } else {
            lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.item.click-to-accept")));
        }

        meta.getPersistentDataContainer().set(contractIdKey, PersistentDataType.STRING, contract.getId());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private void addNavigationButtons(Inventory gui, ContractType type) {
        ItemStack backButton = createPane(Material.BARRIER, plugin.getLangManager().getMessage("gui.submenu.lore.back"));
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.getPersistentDataContainer().set(guiActionKey, PersistentDataType.STRING, "back_to_main");
        backButton.setItemMeta(backMeta);
        gui.setItem(49, backButton);

        if (type != null) {
            String configPath = type.name().toLowerCase() + "-missions.selectable-amount";
            int amount = plugin.getConfig().getInt(configPath, 0);
            String loreLine = plugin.getLangManager().getMessage("gui.submenu.lore.can-accept").replace("%amount%", String.valueOf(amount));
            
            ItemStack infoButton = createPane(Material.OAK_SIGN, "<yellow>Información");
            ItemMeta infoMeta = infoButton.getItemMeta();
            infoMeta.setLore(Collections.singletonList(MessageUtils.parse(loreLine)));
            infoButton.setItemMeta(infoMeta);
            gui.setItem(48, infoButton);
        }
    }

    private ItemStack createPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(MessageUtils.parse(name));
        pane.setItemMeta(meta);
        return pane;
    }

    private void fillBorders(Inventory gui, ItemStack pane) {
        int size = gui.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                if (gui.getItem(i) == null) {
                    gui.setItem(i, pane);
                }
            }
        }
    }
}