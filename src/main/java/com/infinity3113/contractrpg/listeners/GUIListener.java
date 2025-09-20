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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class GUIListener implements Listener {

    private final ContractRPG plugin;
    private final NamespacedKey contractIdKey;

    public GUIListener(ContractRPG plugin) {
        this.plugin = plugin;
        this.contractIdKey = new NamespacedKey(plugin, "contract-id");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // ===== LÓGICA DE CLIC CORREGIDA =====
        // Verificamos si el título del inventario corresponde a ALGUNO de los submenús de contratos.
        // Esto restaura la funcionalidad y evita que el listener se active en el menú principal.
        String dailyTitle = MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.daily"));
        String weeklyTitle = MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.weekly"));
        String specialTitle = MessageUtils.parse(plugin.getLangManager().getMessage("gui.submenu.title.special"));

        if (!title.equals(dailyTitle) && !title.equals(weeklyTitle) && !title.equals(specialTitle)) {
            return; // Si no es un submenú de contratos, no hacemos nada.
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (!clickedItem.hasItemMeta()) return;

        PersistentDataContainer container = clickedItem.getItemMeta().getPersistentDataContainer();
        // Si el ítem no tiene un ID de contrato (como el botón de "atrás"), no hacemos nada.
        if (!container.has(contractIdKey, PersistentDataType.STRING)) {
            return;
        }

        String contractId = container.get(contractIdKey, PersistentDataType.STRING);
        Contract contract = plugin.getContractManager().getContract(contractId);
        if (contract == null) {
            return;
        }

        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData == null) return;

        // Si el jugador ya tiene el contrato, no hacer nada.
        if(playerData.getActiveContracts().containsKey(contractId)){
            return;
        }

        // Se restaura el mensaje correcto para el requisito de nivel.
        if(playerData.getLevel() < contract.getLevelRequirement()){
             // En tu es.yml original no tenías este mensaje, puedes añadirlo:
             // not-enough-level: "%prefix%<red>Tu nivel es demasiado bajo. Requieres nivel %level%."
             MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("not-enough-level")
                .replace("%level%", String.valueOf(contract.getLevelRequirement())));
            player.closeInventory();
            return;
        }

        int currentAccepted = 0;
        int maxSelectable = 1;
        String limitMessage = "";
        ContractType type = contract.getContractType();
        
        // Esta lógica ahora funcionará correctamente porque el listener solo se activa en los menús correctos.
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

        playerData.setContractProgress(contractId, 0); 
        
        MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("contract_accepted")
            .replace("%mission%", contract.getDisplayName()));

        player.closeInventory();
    }
}