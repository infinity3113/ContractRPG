package com.infinity3113.contractrpg.listeners;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.contracts.Contract;
import com.infinity3113.contractrpg.contracts.ContractType;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.util.MessageUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class GUIListener implements Listener {

    private final ContractRPG plugin;
    private final NamespacedKey contractIdKey;
    private final NamespacedKey guiActionKey;

    public GUIListener(ContractRPG plugin) {
        this.plugin = plugin;
        this.contractIdKey = new NamespacedKey(plugin, "contract-id");
        this.guiActionKey = new NamespacedKey(plugin, "gui-action");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Lista de títulos de GUIs válidos
        List<String> validTitles = Arrays.asList(
                MessageUtils.parse(plugin.getLangManager().getMessage("gui.main.title")),
                MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.daily")),
                MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.weekly")),
                MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.special")),
                MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.active"))
        );

        if (!validTitles.contains(title)) {
            return; // No es una de nuestras GUIs, no hacer nada
        }

        event.setCancelled(true); // Es nuestra GUI, cancelar el evento

        ItemStack clickedItem = event.getCurrentItem();
        if (!clickedItem.hasItemMeta()) return;
        ItemMeta meta = clickedItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // 1. GESTIONAR ACCIONES DE NAVEGACIÓN
        if (container.has(guiActionKey, PersistentDataType.STRING)) {
            String action = container.get(guiActionKey, PersistentDataType.STRING);
            if (action == null) return;

            switch (action) {
                case "open_daily":
                    plugin.getGuiManager().openContractList(player, ContractType.DAILY);
                    break;
                case "open_weekly":
                    plugin.getGuiManager().openContractList(player, ContractType.WEEKLY);
                    break;
                case "open_special":
                    plugin.getGuiManager().openContractList(player, ContractType.SPECIAL);
                    break;
                case "open_active":
                    plugin.getGuiManager().openActiveContracts(player);
                    break;
                case "go_back":
                    plugin.getGuiManager().openMainGUI(player);
                    break;
            }
            return;
        }

        // 2. GESTIONAR CLIC PARA ACEPTAR CONTRATO
        if (container.has(contractIdKey, PersistentDataType.STRING)) {
            String contractId = container.get(contractIdKey, PersistentDataType.STRING);
            Contract contract = plugin.getContractManager().getContract(contractId);
            if (contract == null) return;

            PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
            if (playerData == null) return;

            if (playerData.getActiveContracts().containsKey(contractId)) {
                return; // El jugador ya tiene este contrato activo.
            }

            if (playerData.getLevel() < contract.getLevelRequirement()) {
                 MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("not-enough-level")
                    .replace("%level%", String.valueOf(contract.getLevelRequirement())));
                player.closeInventory();
                return;
            }
            
            // Comprobar límite de contratos
            long currentAccepted = playerData.getActiveContracts().keySet().stream()
                    .map(id -> plugin.getContractManager().getContract(id))
                    .filter(c -> c != null && c.getContractType() == contract.getContractType())
                    .count();
            
            String selectableAmountPath = contract.getContractType().name().toLowerCase() + "-missions.selectable-amount";
            int maxSelectable = plugin.getConfig().getInt(selectableAmountPath, 1);
            
            if (currentAccepted >= maxSelectable) {
                MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("contract_limit_reached"));
                return;
            }

            playerData.setContractProgress(contractId, 0); 
            MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("contract_accepted")
                .replace("%mission%", contract.getDisplayName()));
            player.closeInventory();
        }
    }
}