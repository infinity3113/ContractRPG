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

    public GUIManager(ContractRPG plugin) {
        this.plugin = plugin;
        this.contractIdKey = new NamespacedKey(plugin, "contract-id");
        this.guiActionKey = new NamespacedKey(plugin, "gui-action");
    }

    // --- MENÚ PRINCIPAL ---
    public void openMainMenu(Player player) {
        String title = MessageUtils.parse(plugin.getLangManager().getMessage("gui.main.title"));
        Inventory gui = Bukkit.createInventory(null, 27, title);

        // Ítem Contratos Diarios
        gui.setItem(11, createMenuItem(Material.WRITABLE_BOOK, "gui.main.daily", "open_daily"));
        // Ítem Contratos Semanales
        gui.setItem(12, createMenuItem(Material.WRITABLE_BOOK, "gui.main.weekly", "open_weekly"));
        // Ítem Contratos Especiales
        gui.setItem(13, createMenuItem(Material.WRITABLE_BOOK, "gui.main.special", "open_special"));
        // Ítem Mis Contratos Activos
        gui.setItem(15, createMenuItem(Material.BOOK, "gui.main.active", "open_active"));

        player.openInventory(gui);
    }

    // --- SUB-MENÚS (LISTAS DE CONTRATOS) ---
    public void openContractList(Player player, ContractType type) {
        String titleKey = "gui.submenu.title." + type.name().toLowerCase();
        String title = MessageUtils.parse(plugin.getLangManager().getMessage(titleKey));
        Inventory gui = Bukkit.createInventory(null, 54, title);

        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        List<Contract> contracts = plugin.getContractManager().getContractsByType(type);

        for (Contract contract : contracts) {
            // Solo mostrar si no está activo
            if (!playerData.getActiveContracts().containsKey(contract.getId())) {
                 gui.addItem(createContractItem(contract, playerData, false));
            }
        }

        addNavigationButtons(gui, type);
        player.openInventory(gui);
    }

    // --- MENÚ DE CONTRATOS ACTIVOS ---
    public void openActiveContracts(Player player) {
        String title = MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.active"));
        Inventory gui = Bukkit.createInventory(null, 54, title);
        
        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        
        for (String contractId : playerData.getActiveContracts().keySet()) {
            Contract contract = plugin.getContractManager().getContract(contractId);
            if (contract != null) {
                gui.addItem(createContractItem(contract, playerData, true));
            }
        }

        addNavigationButtons(gui, null);
        player.openInventory(gui);
    }


    // --- MÉTODOS DE AYUDA PARA CREAR ÍTEMS ---

    private ItemStack createMenuItem(Material material, String langPath, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtils.parse(plugin.getLangManager().getMessage(langPath + ".name")));

        // Obtener lore del archivo de idioma y parsear colores
        List<String> loreLines = plugin.getLangManager().getMessage(langPath + ".lore").lines().collect(Collectors.toList());
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

        if (isActive) {
            int progress = data.getContractProgress(contract.getId());
            int total = contract.getMissionRequirement();
            // Usando los nuevos colores para la barra
            lore.add(ProgressBar.create(progress, total, 20, "⎜", "&a", "&7"));
        } else {
            lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.item.click-to-accept")));
        }

        meta.getPersistentDataContainer().set(contractIdKey, PersistentDataType.STRING, contract.getId());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addNavigationButtons(Inventory gui, ContractType type) {
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.lore.back")));
        backMeta.getPersistentDataContainer().set(guiActionKey, PersistentDataType.STRING, "back_to_main");
        backButton.setItemMeta(backMeta);
        gui.setItem(49, backButton);

        if (type != null) {
            ItemStack infoButton = new ItemStack(Material.OAK_SIGN);
            ItemMeta infoMeta = infoButton.getItemMeta();
            infoMeta.setDisplayName(MessageUtils.parse("<yellow>Información"));
            
            String configPath = type.name().toLowerCase() + "-missions.selectable-amount";
            int amount = plugin.getConfig().getInt(configPath, 0);
            
            String loreLine = plugin.getLangManager().getMessage("gui.submenu.lore.can-accept").replace("%amount%", String.valueOf(amount));
            infoMeta.setLore(Collections.singletonList(MessageUtils.parse(loreLine)));
            
            infoButton.setItemMeta(infoMeta);
            gui.setItem(48, infoButton);
        }
    }
}