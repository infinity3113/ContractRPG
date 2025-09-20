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
import java.util.Map;
import java.util.stream.Collectors;

public class GUIManager {

    private final ContractRPG plugin;
    public final NamespacedKey contractIdKey;
    public final NamespacedKey guiActionKey;
    private final ItemStack borderPane;

    public GUIManager(ContractRPG plugin) {
        this.plugin = plugin;
        this.contractIdKey = new NamespacedKey(plugin, "contract-id");
        this.guiActionKey = new NamespacedKey(plugin, "gui-action");
        
        // Se mantiene tu lógica para crear los paneles decorativos
        this.borderPane = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
    }

    /**
     * Este es el método que faltaba. Abre el menú principal y utiliza tu método fillBorders.
     */
    public void openMainGUI(Player player) {
        String title = MessageUtils.parse(plugin.getLangManager().getMessage("gui.main.title"));
        int size = plugin.getConfig().getInt("gui-settings.main.size", 27);
        Inventory gui = Bukkit.createInventory(null, size, title);

        // --- Items del Menú Principal (tu lógica original con claves de acción corregidas) ---
        ItemStack daily = new ItemStack(Material.valueOf(plugin.getConfig().getString("gui-settings.main.daily-item.material", "CLOCK")));
        ItemMeta dailyMeta = daily.getItemMeta();
        dailyMeta.setDisplayName(MessageUtils.parse(plugin.getLangManager().getMessage("gui.main.daily-item.name")));
        dailyMeta.setLore(plugin.getLangManager().getLore("gui.main.daily-item.lore").stream().map(MessageUtils::parse).collect(Collectors.toList()));
        dailyMeta.getPersistentDataContainer().set(guiActionKey, PersistentDataType.STRING, "open_daily");
        daily.setItemMeta(dailyMeta);
        gui.setItem(plugin.getConfig().getInt("gui-settings.main.daily-item.slot", 10), daily);

        ItemStack weekly = new ItemStack(Material.valueOf(plugin.getConfig().getString("gui-settings.main.weekly-item.material", "WRITABLE_BOOK")));
        ItemMeta weeklyMeta = weekly.getItemMeta();
        weeklyMeta.setDisplayName(MessageUtils.parse(plugin.getLangManager().getMessage("gui.main.weekly-item.name")));
        weeklyMeta.setLore(plugin.getLangManager().getLore("gui.main.weekly-item.lore").stream().map(MessageUtils::parse).collect(Collectors.toList()));
        weeklyMeta.getPersistentDataContainer().set(guiActionKey, PersistentDataType.STRING, "open_weekly");
        weekly.setItemMeta(weeklyMeta);
        gui.setItem(plugin.getConfig().getInt("gui-settings.main.weekly-item.slot", 12), weekly);

        ItemStack special = new ItemStack(Material.valueOf(plugin.getConfig().getString("gui-settings.main.special-item.material", "NETHER_STAR")));
        ItemMeta specialMeta = special.getItemMeta();
        specialMeta.setDisplayName(MessageUtils.parse(plugin.getLangManager().getMessage("gui.main.special-item.name")));
        specialMeta.setLore(plugin.getLangManager().getLore("gui.main.special-item.lore").stream().map(MessageUtils::parse).collect(Collectors.toList()));
        specialMeta.getPersistentDataContainer().set(guiActionKey, PersistentDataType.STRING, "open_special");
        special.setItemMeta(specialMeta);
        gui.setItem(plugin.getConfig().getInt("gui-settings.main.special-item.slot", 14), special);

        ItemStack active = new ItemStack(Material.valueOf(plugin.getConfig().getString("gui-settings.main.active-contracts-item.material", "BOOK")));
        ItemMeta activeMeta = active.getItemMeta();
        activeMeta.setDisplayName(MessageUtils.parse(plugin.getLangManager().getMessage("gui.main.active-contracts-item.name")));
        activeMeta.setLore(plugin.getLangManager().getLore("gui.main.active-contracts-item.lore").stream().map(MessageUtils::parse).collect(Collectors.toList()));
        activeMeta.getPersistentDataContainer().set(guiActionKey, PersistentDataType.STRING, "open_active");
        active.setItemMeta(activeMeta);
        gui.setItem(plugin.getConfig().getInt("gui-settings.main.active-contracts-item.slot", 16), active);
        
        // Se utiliza tu método para rellenar los bordes
        fillBorders(gui, borderPane);
        
        player.openInventory(gui);
    }

    public void openContractList(Player player, ContractType type) {
        String titleKey = "gui.submenu.title." + type.name().toLowerCase();
        String title = MessageUtils.parse(plugin.getLangManager().getMessage(titleKey));
        int size = plugin.getConfig().getInt("gui-settings.submenu.size", 54);
        Inventory gui = Bukkit.createInventory(null, size, title);

        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData == null) return;

        List<Contract> availableContracts = plugin.getContractManager().getAvailableContracts(playerData.getLevel(), type);
        for (int i = 0; i < availableContracts.size() && i < 45; i++) { // Límite para dejar espacio para navegación
            Contract contract = availableContracts.get(i);
            ItemStack item = createContractItem(contract, playerData);
            gui.setItem(i, item);
        }
        
        addNavigation(gui, type); // Usamos tu método de navegación
        fillBorders(gui, borderPane);

        player.openInventory(gui);
    }

    public void openActiveContracts(Player player) {
        String title = MessageUtils.parse(plugin.getLangManager().getMessage("gui.active.title"));
        int size = plugin.getConfig().getInt("gui-settings.active.size", 54);
        Inventory gui = Bukkit.createInventory(null, size, title);
        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());

        if (playerData != null && !playerData.getActiveContracts().isEmpty()) {
            int i = 0;
            for (Map.Entry<String, Integer> entry : playerData.getActiveContracts().entrySet()) {
                if (i >= 45) break;
                Contract contract = plugin.getContractManager().getContract(entry.getKey());
                if (contract != null) {
                    ItemStack item = createActiveContractItem(contract, entry.getValue());
                    gui.setItem(i, item);
                    i++;
                }
            }
        } else {
            ItemStack noContracts = new ItemStack(Material.valueOf(plugin.getConfig().getString("gui-settings.active.no-contracts-item.material", "BARRIER")));
            ItemMeta meta = noContracts.getItemMeta();
            meta.setDisplayName(MessageUtils.parse(plugin.getLangManager().getMessage("gui.active.no-contracts-item.name")));
            meta.setLore(plugin.getLangManager().getLore("gui.active.no-contracts-item.lore").stream().map(MessageUtils::parse).collect(Collectors.toList()));
            noContracts.setItemMeta(meta);
            gui.setItem(plugin.getConfig().getInt("gui-settings.active.no-contracts-item.slot", 22), noContracts);
        }

        addNavigation(gui, null); // Pasamos null porque no es un submenú de tipo específico
        fillBorders(gui, borderPane);

        player.openInventory(gui);
    }
    
    // El resto de tus métodos se mantienen intactos
    private ItemStack createContractItem(Contract contract, PlayerData playerData) {
        Material material = Material.valueOf(plugin.getConfig().getString("gui-settings.submenu.item-material", "PAPER"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.parse(contract.getDisplayName()));

        List<String> lore = new ArrayList<>();
        plugin.getLangManager().getLore("gui.submenu.item-lore").forEach(line -> {
            line = line.replace("%description%", String.join("\n", contract.getDescription()))
                    .replace("%rewards%", String.join(", ", contract.getDisplayRewards()))
                    .replace("%exp%", String.valueOf(contract.getExperienceReward()))
                    .replace("%points%", String.valueOf(contract.getContractPointsReward()))
                    .replace("%level%", String.valueOf(contract.getLevelRequirement()));
            lore.add(MessageUtils.parse(line));
        });

        if (playerData.getActiveContracts().containsKey(contract.getId())) {
            plugin.getLangManager().getLore("gui.submenu.lore-already-accepted").forEach(line -> lore.add(MessageUtils.parse(line)));
        } else if (playerData.getLevel() < contract.getLevelRequirement()) {
            plugin.getLangManager().getLore("gui.submenu.lore-level-too-low").forEach(line -> lore.add(MessageUtils.parse(line.replace("%level%", String.valueOf(contract.getLevelRequirement())))));
        } else {
            plugin.getLangManager().getLore("gui.submenu.lore-can-accept").forEach(line -> lore.add(MessageUtils.parse(line)));
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(contractIdKey, PersistentDataType.STRING, contract.getId());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActiveContractItem(Contract contract, int progress) {
        Material material = Material.valueOf(plugin.getConfig().getString("gui-settings.active.item-material", "WRITTEN_BOOK"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.parse(contract.getDisplayName()));

        List<String> lore = new ArrayList<>();
        String progressBar = ProgressBar.create(progress, contract.getMissionRequirement());

        plugin.getLangManager().getLore("gui.active.item-lore").forEach(line -> {
            line = line.replace("%description%", String.join("\n", contract.getDescription()))
                    .replace("%progress%", progressBar)
                    .replace("%progress_current%", String.valueOf(progress))
                    .replace("%progress_max%", String.valueOf(contract.getMissionRequirement()))
                    .replace("%rewards%", String.join(", ", contract.getDisplayRewards()));
            lore.add(MessageUtils.parse(line));
        });

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    // Tu método de navegación, modificado para usar la acción correcta "go_back"
    private void addNavigation(Inventory gui, ContractType type) {
        ItemStack backButton = createPane(Material.ARROW, plugin.getLangManager().getMessage("gui.back-button.name"));
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.getPersistentDataContainer().set(guiActionKey, PersistentDataType.STRING, "go_back"); // Acción corregida
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

    // Tu método para crear paneles
    private ItemStack createPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(MessageUtils.parse(name));
        pane.setItemMeta(meta);
        return pane;
    }

    // Tu método para rellenar bordes
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