package com.example.contractrpg.gui;

import com.example.contractrpg.ContractRPG;
import com.example.contractrpg.contracts.Contract;
import com.example.contractrpg.data.PlayerData;
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

public class ContractGUI implements Listener {

    private final ContractRPG plugin;
    private final Player player;
    private final PlayerData playerData;

    public ContractGUI(ContractRPG plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = plugin.getContractManager().getPlayerData(player);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        Component titleComponent = MiniMessage.miniMessage().deserialize(plugin.getLangManager().getMessage("gui_title"));
        String legacyTitle = LegacyComponentSerializer.legacySection().serialize(titleComponent);
        Inventory gui = Bukkit.createInventory(null, 54, legacyTitle);

        populateItems(gui);

        player.openInventory(gui);
    }

    private void populateItems(Inventory gui) {
        gui.clear(); // Limpia el inventario antes de volver a llenarlo
        
        int acceptedDaily = (int) playerData.getActiveContracts().stream().filter(c -> c.getContractType() == com.example.contractrpg.contracts.ContractType.DAILY).count();
        int maxDaily = plugin.getConfig().getInt("daily-missions.selectable-amount", 2);

        ItemStack availableTitle = createItem(Material.EMERALD_BLOCK, plugin.getLangManager().getMessage("gui_available_title").replace("%accepted%", String.valueOf(acceptedDaily)).replace("%max%", String.valueOf(maxDaily)));
        gui.setItem(10, availableTitle);
        
        int availableSlot = 19;
        for (Contract contract : playerData.getAvailableDailyContracts()) {
            if (availableSlot > 23) break; // Límite de slots para disponibles
            gui.setItem(availableSlot++, createContractItem(contract, false));
        }

        ItemStack activeTitle = createItem(Material.DIAMOND_BLOCK, plugin.getLangManager().getMessage("gui_active_title"));
        gui.setItem(16, activeTitle);

        int activeSlot = 25;
        for (Contract contract : playerData.getActiveContracts()) {
            if (activeSlot > 29) break; // Límite de slots para activos
            gui.setItem(activeSlot++, createContractItem(contract, true));
        }
    }
    
    private ItemStack createContractItem(Contract contract, boolean isActive) {
        Material material = isActive ? Material.WRITABLE_BOOK : Material.BOOK;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String missionKey = contract.getMissionType().name();
        String missionFormat = plugin.getLangManager().getMessage("mission-formats." + missionKey);
        
        String targetKey = "translation-keys." + contract.getTarget().replace(":", ".");
        String translatedTarget = plugin.getLangManager().getMessage(targetKey);

        String missionName = missionFormat
                .replace("%amount%", String.valueOf(contract.getRequiredAmount()))
                .replace("%target%", translatedTarget);
        
        Component nameComponent = MiniMessage.miniMessage().deserialize(missionName);
        meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(nameComponent));

        List<Component> componentLore = new ArrayList<>();
        componentLore.add(Component.text("")); 

        String progressBar = ProgressBar.create(contract.getCurrentAmount(), contract.getRequiredAmount(), 20, "▌", "<green>", "<gray>");
        componentLore.add(MiniMessage.miniMessage().deserialize(plugin.getLangManager().getMessage("gui_progress")
                .replace("%current%", String.valueOf(contract.getCurrentAmount()))
                .replace("%required%", String.valueOf(contract.getRequiredAmount()))
        ));
        componentLore.add(MiniMessage.miniMessage().deserialize(progressBar));
        componentLore.add(Component.text(""));

        componentLore.add(MiniMessage.miniMessage().deserialize(plugin.getLangManager().getMessage("gui_reward")
                .replace("%reward%", String.valueOf(contract.getReward()))
        ));

        if (!isActive) {
            componentLore.add(Component.text(""));
            componentLore.add(MiniMessage.miniMessage().deserialize(plugin.getLangManager().getMessage("gui_click_to_accept")));
        }

        List<String> legacyLore = new ArrayList<>();
        for (Component line : componentLore) {
            legacyLore.add(LegacyComponentSerializer.legacySection().serialize(line));
        }
        meta.setLore(legacyLore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(name)));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getInventory().getHolder() != null) return;
        String legacyTitle = LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(plugin.getLangManager().getMessage("gui_title")));
        if (!event.getView().getTitle().equals(legacyTitle)) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (clickedItem.getType() == Material.BOOK) {
            int slot = event.getSlot();
            int availableContractIndex = slot - 19;

            if (availableContractIndex >= 0 && availableContractIndex < playerData.getAvailableDailyContracts().size()) {
                Contract contractToAccept = playerData.getAvailableDailyContracts().get(availableContractIndex);
                
                boolean accepted = plugin.getContractManager().acceptContract(player, contractToAccept);

                if (accepted) {
                    populateItems(event.getInventory());
                } else {
                    player.closeInventory();
                }
            }
        }
    }
}