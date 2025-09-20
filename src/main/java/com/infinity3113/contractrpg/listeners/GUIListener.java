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
        // Verificaciones básicas
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        // Siempre cancelamos el evento si el inventario pertenece al plugin para evitar que los jugadores tomen los ítems.
        // Verificamos si el título del inventario es uno de los nuestros.
        String title = event.getView().getTitle();
        String mainTitle = MessageUtils.parse(plugin.getLangManager().getMessage("gui.main.title"));
        String dailyTitle = MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.daily"));
        String weeklyTitle = MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.weekly"));
        String specialTitle = MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.special"));
        String activeTitle = MessageUtils.parse(plugin.getLangManager().getMessage("gui.active.title"));

        boolean isContractGui = title.equals(mainTitle) || title.equals(dailyTitle) || title.equals(weeklyTitle) || title.equals(specialTitle) || title.equals(activeTitle);
        
        if (!isContractGui) {
            return; // Si no es un menú del plugin, no hacemos nada.
        }

        event.setCancelled(true);

        if (!clickedItem.hasItemMeta()) return;
        ItemMeta meta = clickedItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // 1. GESTIONAR ACCIONES DE LA GUI (Botones de navegación)
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
            return; // Acción de GUI ejecutada, terminamos aquí.
        }

        // 2. GESTIONAR CLIC EN UN CONTRATO (Para aceptarlo)
        if (container.has(contractIdKey, PersistentDataType.STRING)) {
            String contractId = container.get(contractIdKey, PersistentDataType.STRING);
            Contract contract = plugin.getContractManager().getContract(contractId);
            if (contract == null) return;

            PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
            if (playerData == null) return;

            // Si el jugador ya tiene el contrato, no hacer nada.
            if (playerData.getActiveContracts().containsKey(contractId)) {
                return;
            }

            // Comprobar requisito de nivel
            if (playerData.getLevel() < contract.getLevelRequirement()) {
                 MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("not-enough-level")
                    .replace("%level%", String.valueOf(contract.getLevelRequirement())));
                player.closeInventory();
                return;
            }
            
            // Comprobar límite de contratos aceptados para ese tipo
            int currentAccepted = 0;
            int maxSelectable = 1;
            String limitMessage = "";
            ContractType type = contract.getContractType();
            
            if (type == ContractType.DAILY) {
                maxSelectable = plugin.getConfig().getInt("daily-missions.selectable-amount", 1);
                limitMessage = plugin.getLangManager().getMessage("contract_limit_reached");
            } else if (type == ContractType.WEEKLY) {
                maxSelectable = plugin.getConfig().getInt("weekly-missions.selectable-amount", 1);
                limitMessage = plugin.getLangManager().getMessage("contract_limit_reached");
            } else if (type == ContractType.SPECIAL) {
                maxSelectable = plugin.getConfig().getInt("special-missions.selectable-amount", 1);
                limitMessage = plugin.getLangManager().getMessage("contract_limit_reached");
            }

            for (String activeId : playerData.getActiveContracts().keySet()) {
                Contract activeContract = plugin.getContractManager().getContract(activeId);
                if (activeContract != null && activeContract.getContractType() == type) {
                    currentAccepted++;
                }
            }
            
            if (currentAccepted >= maxSelectable) {
                MessageUtils.sendMessage(player, limitMessage);
                return;
            }

            // Aceptar el contrato
            playerData.setContractProgress(contractId, 0); 
            
            MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("contract_accepted")
                .replace("%mission%", contract.getDisplayName()));

            player.closeInventory();
        }
    }
}