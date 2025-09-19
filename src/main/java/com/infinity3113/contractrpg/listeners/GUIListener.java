package com.infinity3113.contractrpg.listeners;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.contracts.Contract;
import com.infinity3113.contractrpg.contracts.ContractType;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.managers.GUIManager;
import com.infinity3113.contractrpg.util.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class GUIListener implements Listener {

    private final ContractRPG plugin;
    private final GUIManager guiManager;

    public GUIListener(ContractRPG plugin) {
        this.plugin = plugin;
        this.guiManager = plugin.getGuiManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Verificar si el inventario es parte del plugin
        if (event.getView().getTitle() == null || event.getCurrentItem() == null) {
            return;
        }

        // Prevenir que los jugadores tomen ítems
        if (isContractGui(event.getView().getTitle())) {
            event.setCancelled(true);
        } else {
            return; // No es una GUI de este plugin, no hacer nada más
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemMeta meta = clickedItem.getItemMeta();

        if (meta == null) return;

        // --- MANEJO DE ACCIONES DE NAVEGACIÓN ---
        if (meta.getPersistentDataContainer().has(guiManager.guiActionKey, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(guiManager.guiActionKey, PersistentDataType.STRING);
            switch (action) {
                case "open_daily":
                    guiManager.openContractList(player, ContractType.DAILY);
                    return;
                case "open_weekly":
                    guiManager.openContractList(player, ContractType.WEEKLY);
                    return;
                case "open_special":
                    guiManager.openContractList(player, ContractType.SPECIAL);
                    return;
                case "open_active":
                    guiManager.openActiveContracts(player);
                    return;
                case "back_to_main":
                    guiManager.openMainMenu(player);
                    return;
            }
        }

        // --- MANEJO DE ACEPTACIÓN DE CONTRATOS ---
        if (meta.getPersistentDataContainer().has(guiManager.contractIdKey, PersistentDataType.STRING)) {
            acceptContract(player, meta);
        }
    }

    private void acceptContract(Player player, ItemMeta meta) {
        String contractId = meta.getPersistentDataContainer().get(guiManager.contractIdKey, PersistentDataType.STRING);
        
        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        Contract contract = plugin.getContractManager().getContract(contractId);

        if (playerData == null || contract == null) return;

        if (playerData.getActiveContracts().containsKey(contractId)) return;

        // Lógica de verificación de límites
        ContractType type = contract.getContractType();
        String configPath = type.name().toLowerCase() + "-missions.selectable-amount";
        int maxContracts = plugin.getConfig().getInt(configPath, 1);

        long currentContracts = playerData.getActiveContracts().keySet().stream()
            .map(id -> plugin.getContractManager().getContract(id))
            .filter(c -> c != null && c.getContractType() == type)
            .count();

        if (currentContracts >= maxContracts) {
            MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("contract_limit_reached"));
            return;
        }

        playerData.addContract(contractId);
        MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("contract_accepted"));
        player.closeInventory();
    }

    private boolean isContractGui(String title) {
        // Comprueba si el título coincide con cualquiera de las GUIs del plugin
        return title.equals(MessageUtils.parse(plugin.getLangManager().getMessage("gui.main.title"))) ||
               title.equals(MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.daily"))) ||
               title.equals(MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.weekly"))) ||
               title.equals(MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.special"))) ||
               title.equals(MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.active")));
    }
}