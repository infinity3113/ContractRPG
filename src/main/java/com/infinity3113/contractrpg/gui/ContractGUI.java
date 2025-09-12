package com.example.contractrpg.gui;

import com.example.contractrpg.ContractRPG;
import com.example.contractrpg.contracts.Contract;
import com.example.contractrpg.contracts.ContractManager;
import com.example.contractrpg.contracts.MissionType;
import com.example.contractrpg.data.PlayerData;
import com.example.contractrpg.managers.LangManager;
import com.example.contractrpg.util.ProgressBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContractGUI implements Listener {

    private final ContractRPG plugin;
    private final Player player;
    private final LangManager lang;
    private final ContractManager contractManager;

    public ContractGUI(ContractRPG plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.lang = plugin.getLangManager();
        this.contractManager = plugin.getContractManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMainMenu() {
        String title = formatString(lang.getMessage("gui.main_menu.title"));
        Inventory gui = Bukkit.createInventory(player, 27, title);

        gui.setItem(11, createGuiItem(Material.WRITABLE_BOOK, "gui.main_menu.active_contracts.name", "gui.main_menu.active_contracts.lore"));
        gui.setItem(13, createGuiItem(Material.SUNFLOWER, "gui.main_menu.daily_contracts.name", "gui.main_menu.daily_contracts.lore"));
        gui.setItem(15, createGuiItem(Material.CLOCK, "gui.main_menu.weekly_contracts.name", "gui.main_menu.weekly_contracts.lore"));

        fillEmptySlots(gui, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        player.openInventory(gui);
    }

    private void openContractList(String type) {
        PlayerData playerData = contractManager.getPlayerData(player);
        List<Contract> contractsToShow;
        String titleKey;
        boolean isAcceptableList;

        switch (type) {
            case "ACTIVE":
                titleKey = "gui.active_contracts.title";
                contractsToShow = playerData.getActiveContracts();
                isAcceptableList = false;
                break;
            case "DAILY":
                titleKey = "gui.daily_contracts.title";
                contractsToShow = playerData.getAvailableDailyContracts();
                isAcceptableList = true;
                break;
            case "WEEKLY":
                titleKey = "gui.weekly_contracts.title";
                contractsToShow = playerData.getAvailableWeeklyContracts();
                isAcceptableList = true;
                break;
            default:
                return;
        }

        String title = formatString(lang.getMessage(titleKey));
        Inventory gui = Bukkit.createInventory(player, 54, title);

        for (int i = 0; i < contractsToShow.size() && i < 45; i++) {
            Contract contract = contractsToShow.get(i);
            gui.setItem(i, createContractItem(contract, isAcceptableList));
        }

        gui.setItem(49, createGuiItem(Material.ARROW, "gui.back_button.name", "gui.back_button.lore"));
        fillEmptySlots(gui, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() == null || !event.getView().getPlayer().getUniqueId().equals(player.getUniqueId())) return;

        String title = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        event.setCancelled(true);

        String mainMenuTitle = formatString(lang.getMessage("gui.main_menu.title"));
        String dailyContractsTitle = formatString(lang.getMessage("gui.daily_contracts.title"));
        String weeklyContractsTitle = formatString(lang.getMessage("gui.weekly_contracts.title"));

        if (title.equals(mainMenuTitle)) {
            if (clickedItem.getType() == Material.WRITABLE_BOOK) openContractList("ACTIVE");
            else if (clickedItem.getType() == Material.SUNFLOWER) openContractList("DAILY");
            else if (clickedItem.getType() == Material.CLOCK) openContractList("WEEKLY");
        }
        else if (title.equals(dailyContractsTitle)) {
            handleContractAcceptance(clickedItem, contractManager.getPlayerData(player).getAvailableDailyContracts());
        }
        else if (title.equals(weeklyContractsTitle)) {
            handleContractAcceptance(clickedItem, contractManager.getPlayerData(player).getAvailableWeeklyContracts());
        }
        
        if (clickedItem.getType() == Material.ARROW) {
            openMainMenu();
        }
    }

    private void handleContractAcceptance(ItemStack clickedItem, List<Contract> availableContracts) {
        if (clickedItem.getType() == Material.ARROW) {
            openMainMenu();
            return;
        }

        if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasLore()) {
            List<String> lore = clickedItem.getItemMeta().getLore();
            if (lore == null || lore.isEmpty()) return;

            for (Contract contract : availableContracts) {
                String missionLine = formatString(getFormattedMissionLine(contract));
                if (lore.get(0).equals(missionLine)) {
                    if (contractManager.acceptContract(player, contract)) {
                        player.closeInventory();
                    }
                    return;
                }
            }
        }
    }

    private ItemStack createContractItem(Contract contract, boolean isAcceptable) {
        Material material = getMaterialForMission(contract.getMissionType());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add(getFormattedMissionLine(contract));
        lore.add(lang.getMessage("gui.item.reward").replace("%reward%", String.valueOf(contract.getReward())));
        
        if (!isAcceptable) {
            lore.add(" ");
            lore.add(ProgressBar.create(contract.getCurrentAmount(), contract.getRequiredAmount()));
        } else {
            lore.add(" ");
            lore.add(lang.getMessage("gui.item.click_to_accept"));
        }

        meta.setDisplayName(formatString(lang.getMessage("gui.item.contract_title")));
        meta.setLore(lore.stream().map(this::formatString).collect(Collectors.toList()));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuiItem(Material material, String nameKey, String loreKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(formatString(lang.getMessage(nameKey)));
        List<String> lore = lang.getStringList(loreKey).stream().map(this::formatString).collect(Collectors.toList());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillEmptySlots(Inventory inv, ItemStack item) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                ItemStack fillItem = item.clone();
                ItemMeta meta = fillItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(" ");
                    fillItem.setItemMeta(meta);
                }
                inv.setItem(i, fillItem);
            }
        }
    }

    private String getFormattedMissionLine(Contract contract) {
        String missionKey = contract.getMissionType().name();
        String missionFormat = lang.getMessage("mission-formats." + missionKey);
        return missionFormat.replace("%amount%", String.valueOf(contract.getRequiredAmount()))
                .replace("%target%", contract.getTarget().replace("_", " "));
    }

    private Material getMaterialForMission(MissionType type) {
        switch (type) {
            case KILL: case KILL_MYTHIC: return Material.DIAMOND_SWORD;
            case BREAK: return Material.DIAMOND_PICKAXE;
            case FARM: return Material.WHEAT;
            case DELIVER_MMOITEM: return Material.CHEST;
            default: return Material.BOOK;
        }
    }

    private String formatString(String text) {
        Component component = MiniMessage.miniMessage().deserialize(text);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}