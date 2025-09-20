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
import java.util.List;
import java.util.stream.Collectors;

public class GUIManager {

    private final ContractRPG plugin;
    public final NamespacedKey contractIdKey;
    public final NamespacedKey guiActionKey;

    public GUIManager(ContractRPG plugin) {
        this.plugin = plugin;
        this.contractIdKey = new NamespacedKey(plugin, "contract-id");
        this.guiActionKey = new NamespacedKey(plugin, "gui-action");
    }

    public void openMainGUI(Player player) {
        String title = MessageUtils.parse(plugin.getLangManager().getMessage("gui.main.title"));
        Inventory gui = Bukkit.createInventory(null, 27, title);
        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData == null) return;

        gui.setItem(10, createMenuButton(Material.WRITABLE_BOOK, "gui.main.daily", "open_daily"));
        gui.setItem(12, createMenuButton(Material.BOOK, "gui.main.weekly", "open_weekly"));
        gui.setItem(14, createMenuButton(Material.NETHER_STAR, "gui.main.special", "open_special"));
        gui.setItem(16, createMenuButton(Material.COMPASS, "gui.main.active", "open_active"));
        gui.setItem(4, createStatsItem(playerData));

        fillBorders(gui, new ItemStack(Material.BLACK_STAINED_GLASS_PANE), true);
        player.openInventory(gui);
    }

    public void openContractList(Player player, ContractType type) {
        String title = MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title." + type.name().toLowerCase()));
        Inventory gui = Bukkit.createInventory(null, 54, title);
        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData == null) return;

        String offeredAmountPath = type.name().toLowerCase() + "-missions.offered-amount";
        int contractsToOffer = plugin.getConfig().getInt(offeredAmountPath, 3);
        List<Contract> offeredContracts = plugin.getContractManager().getOfferedContracts(playerData, type, contractsToOffer);

        // --- CORRECCIÓN AQUÍ ---
        // Se inicia el contador en el slot 10 (dentro del borde)
        int slot = 10;
        for (Contract contract : offeredContracts) {
            // Se asegura de no salirse del área de contratos
            if (slot >= 44) break;
            gui.setItem(slot, createContractItem(contract, playerData));

            slot++;
            // Si el siguiente slot toca un borde, se lo salta
            if (slot % 9 == 8) {
                slot += 2;
            }
        }

        addNavigationButtons(gui, type, "go_back");
        fillBorders(gui, new ItemStack(Material.GRAY_STAINED_GLASS_PANE), false);
        player.openInventory(gui);
    }

    public void openActiveContracts(Player player) {
        String title = MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.active"));
        Inventory gui = Bukkit.createInventory(null, 54, title);
        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData == null) return;

        // --- CORRECCIÓN AQUÍ ---
        // Se inicia el contador en el slot 10 (dentro del borde)
        int slot = 10;
        if (playerData.getActiveContracts().isEmpty()) {
            // Puedes agregar un ítem de "no tienes contratos" aquí si quieres
        } else {
            for (String contractId : playerData.getActiveContracts().keySet()) {
                // Se asegura de no salirse del área de contratos
                if (slot >= 44) break;
                Contract contract = plugin.getContractManager().getContract(contractId);
                if (contract != null) {
                    gui.setItem(slot, createContractItem(contract, playerData));
                    slot++;
                    // Si el siguiente slot toca un borde, se lo salta
                    if (slot % 9 == 8) {
                        slot += 2;
                    }
                }
            }
        }
        
        addNavigationButtons(gui, null, "go_back");
        fillBorders(gui, new ItemStack(Material.GRAY_STAINED_GLASS_PANE), false);
        player.openInventory(gui);
    }

    private ItemStack createMenuButton(Material material, String langPath, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.parse(plugin.getLangManager().getMessage(langPath + ".name")));
        meta.setLore(plugin.getLangManager().getMessageList(langPath + ".lore").stream().map(MessageUtils::parse).collect(Collectors.toList()));
        meta.getPersistentDataContainer().set(guiActionKey, PersistentDataType.STRING, action);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatsItem(PlayerData playerData) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.parse(plugin.getLangManager().getMessage("gui.main.stats.name")));
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getLangManager().getMessageList("gui.main.stats.lore")) {
            String processedLine = line
                    .replace("%level%", String.valueOf(playerData.getLevel()))
                    .replace("%contract_points%", String.valueOf(playerData.getContractPoints()))
                    .replace("%exp_bar%", ProgressBar.create(playerData.getExperience(), playerData.getRequiredExperience(), 20, "|", "<green>", "<gray>"))
                    .replace("%current_exp%", String.valueOf(playerData.getExperience()))
                    .replace("%required_exp%", String.valueOf(playerData.getRequiredExperience()));
            lore.add(MessageUtils.parse(processedLine));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createContractItem(Contract contract, PlayerData playerData) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.parse(contract.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.addAll(contract.getDescription().stream().map(MessageUtils::parse).collect(Collectors.toList()));
        lore.add(" ");

        if (playerData.getActiveContracts().containsKey(contract.getId())) {
            item.setType(Material.WRITTEN_BOOK);
            int progress = playerData.getContractProgress(contract.getId());
            int required = contract.getMissionRequirement();
            lore.add(MessageUtils.parse(ProgressBar.create(progress, required, 20, "|", "<green>", "<red>")));
            lore.add(MessageUtils.parse("<gray>Progreso: <yellow>" + progress + "/" + required));
        } else {
            lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.item.click-to-accept")));
        }
        
        lore.add(" ");
        lore.add(MessageUtils.parse("<gold>Recompensas:"));
        lore.addAll(contract.getDisplayRewards().stream().map(r -> MessageUtils.parse("<gray>- <white>" + r)).collect(Collectors.toList()));
        if (contract.getExperienceReward() > 0) {
             lore.add(MessageUtils.parse("<gray>- <yellow>" + contract.getExperienceReward() + " EXP"));
        }
        if (contract.getContractPointsReward() > 0) {
            String pointsFormat = plugin.getLangManager().getMessage("gui.submenu.item.contract-points");
            String pointsLine = pointsFormat.replace("%points%", String.valueOf(contract.getContractPointsReward()));
            lore.add(MessageUtils.parse(pointsLine));
        }
        
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(contractIdKey, PersistentDataType.STRING, contract.getId());
        item.setItemMeta(meta);
        return item;
    }
    
    private void addNavigationButtons(Inventory gui, ContractType type, String backAction) {
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.lore.back")));
        backMeta.getPersistentDataContainer().set(guiActionKey, PersistentDataType.STRING, backAction);
        backButton.setItemMeta(backMeta);
        gui.setItem(49, backButton);

        if (type != null) {
            String selectableAmountPath = type.name().toLowerCase() + "-missions.selectable-amount";
            int amount = plugin.getConfig().getInt(selectableAmountPath, 1);
            
            ItemStack infoItem = new ItemStack(Material.OAK_SIGN);
            ItemMeta infoMeta = infoItem.getItemMeta();
            infoMeta.setDisplayName(MessageUtils.parse("<aqua>Información"));
            List<String> lore = new ArrayList<>();
            String canAccept = plugin.getLangManager().getMessage("gui.submenu.lore.can-accept").replace("%amount%", String.valueOf(amount));
            lore.add(MessageUtils.parse(canAccept));
            infoMeta.setLore(lore);
            infoItem.setItemMeta(infoMeta);
            gui.setItem(48, infoItem); // Al lado del botón de volver
        }
    }

    private void fillBorders(Inventory gui, ItemStack pane, boolean isMainMenu) {
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        int size = gui.getSize();
        for (int i = 0; i < size; i++) {
            if (isMainMenu) {
                if (gui.getItem(i) == null) gui.setItem(i, pane);
            } else {
                if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                    if (gui.getItem(i) == null) gui.setItem(i, pane);
                }
            }
        }
    }
}