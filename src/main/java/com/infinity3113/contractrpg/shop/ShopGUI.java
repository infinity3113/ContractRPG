package com.infinity3113.contractrpg.shop;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ShopGUI {

    private final ContractRPG plugin;
    public final NamespacedKey shopItemIdKey;
    public final Map<UUID, Integer> playerShopPages = new HashMap<>();
    public final Map<UUID, Integer> editorPages = new HashMap<>();
    private static final int ITEMS_PER_PAGE = 45;

    public ShopGUI(ContractRPG plugin) {
        this.plugin = plugin;
        this.shopItemIdKey = new NamespacedKey(plugin, "shop-item-id");
    }

    // ... (Clases internas Holder sin cambios) ...
    public static class PlayerShopHolder implements InventoryHolder {
        private final int page;
        public PlayerShopHolder(int page) { this.page = page; }
        public int getPage() { return page; }
        @Override public @NotNull Inventory getInventory() { return null; }
    }
    public static class ShopEditorHolder implements InventoryHolder {
        private final int page;
        public ShopEditorHolder(int page) { this.page = page; }
        public int getPage() { return page; }
        @Override public @NotNull Inventory getInventory() { return null; }
    }
    public static class ItemEditorHolder implements InventoryHolder {
        private final int slot;
        public ItemEditorHolder(int slot) { this.slot = slot; }
        public int getSlot() { return slot; }
        @Override public @NotNull Inventory getInventory() { return null; }
    }

    public void openShop(Player player, int page) {
        playerShopPages.put(player.getUniqueId(), page);
        String baseTitle = plugin.getShopManager().getConfig().getString("shop-title", "<bold><#F52E27>Tienda de Contratos</bold>");
        int size = plugin.getShopManager().getConfig().getInt("shop-size", 54);
        
        List<Map.Entry<Integer, ShopItem>> sortedItems = new ArrayList<>(plugin.getShopManager().getShopItems().entrySet());
        sortedItems.sort(Map.Entry.comparingByKey());

        int maxPages = (int) Math.ceil((double) sortedItems.size() / ITEMS_PER_PAGE);
        String title = MessageUtils.parse(baseTitle + " <dark_gray>(Pág " + (page + 1) + "/" + Math.max(1, maxPages) + ")</dark_gray>");

        Inventory shop = Bukkit.createInventory(new PlayerShopHolder(page), size, title);
        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData == null) return;

        int startIndex = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < sortedItems.size()) {
                Map.Entry<Integer, ShopItem> entry = sortedItems.get(itemIndex);
                shop.setItem(i, createPlayerDisplayItem(entry.getValue(), playerData, entry.getKey()));
            }
        }
        
        addPaginationControls(shop, page, maxPages);
        player.openInventory(shop);
    }

    public void openEditor(Player player, int page) {
        editorPages.put(player.getUniqueId(), page);
        String baseTitle = plugin.getShopManager().getConfig().getString("editor-title", "<bold><#27A6F5>Editor de la Tienda</bold>");
        int size = plugin.getShopManager().getConfig().getInt("shop-size", 54);

        List<Map.Entry<Integer, ShopItem>> sortedItems = new ArrayList<>(plugin.getShopManager().getShopItems().entrySet());
        sortedItems.sort(Map.Entry.comparingByKey());

        int maxPages = (int) Math.ceil((double) sortedItems.size() / ITEMS_PER_PAGE);
        String title = MessageUtils.parse(baseTitle + " <dark_gray>(Pág " + (page + 1) + "/" + Math.max(1, maxPages) + ")</dark_gray>");
        
        Inventory editor = Bukkit.createInventory(new ShopEditorHolder(page), size, title);

        int startIndex = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
             int itemIndex = startIndex + i;
             if(itemIndex < sortedItems.size()){
                 Map.Entry<Integer, ShopItem> entry = sortedItems.get(itemIndex);
                 editor.setItem(i, createEditorDisplayItem(entry.getValue(), entry.getKey()));
             }
        }
        
        addPaginationControls(editor, page, maxPages);
        player.openInventory(editor);
    }

    public void openItemEditor(Player player, int slot, ShopItem shopItem) {
        Inventory itemEditor = Bukkit.createInventory(new ItemEditorHolder(slot), 36, MessageUtils.parse("<dark_gray>Editando Item"));

        itemEditor.setItem(4, shopItem.getItemStack());

        itemEditor.setItem(20, createButton(Material.GOLD_INGOT, "<yellow><bold>Editar Precio</bold></yellow>", Arrays.asList(
                "<gray>Precio actual: <gold>" + shopItem.getPrice() + "</gold>",
                "<white>Click para cambiar."
        )));

        itemEditor.setItem(22, createButton(Material.CHEST, "<aqua><bold>Editar Stock</bold></aqua>", Arrays.asList(
                "<gray>Stock: <blue>" + (shopItem.isInfiniteStock() ? "Infinito" : shopItem.getStock()) + "</blue>",
                "<white>Click IZQ: +1 | Click DER: -1",
                "<white>SHIFT + Click: +/- 10",
                "<white>Click MEDIO: Infinito"
        )));

        itemEditor.setItem(24, createButton(Material.CLOCK, "<#FF8C00><bold>Editar Reabastecimiento</bold></#FF8C00>", Arrays.asList(
                "<gray>Cooldown: <yellow>" + formatTime(shopItem.getCooldown()) + "</yellow>",
                "<white>Click para cambiar (en segundos)."
        )));
        
        itemEditor.setItem(31, createButton(Material.BARRIER, "<red><bold>Volver</bold></red>", null));

        fillBorders(itemEditor, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        player.openInventory(itemEditor);
    }
    
    private void addPaginationControls(Inventory gui, int currentPage, int maxPages) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int i = ITEMS_PER_PAGE; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        if (currentPage > 0) {
            gui.setItem(45, createButton(Material.ARROW, "<green><bold>Página Anterior</bold></green>", null));
        }
        if (currentPage < maxPages - 1) {
            gui.setItem(53, createButton(Material.ARROW, "<green><bold>Página Siguiente</bold></green>", null));
        }
    }

    private ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.parse(name));
        if (lore != null) {
            meta.setLore(lore.stream().map(MessageUtils::parse).collect(Collectors.toList()));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEditorDisplayItem(ShopItem shopItem, int originalSlot) {
        ItemStack display = shopItem.getItemStack().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(display.getType());
        }

        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(MessageUtils.parse(" "));
        lore.add(MessageUtils.parse("<dark_gray>»</dark_gray> <gray>Precio: <gold>" + shopItem.getPrice() + " Puntos</gold>"));
        lore.add(MessageUtils.parse("<dark_gray>»</dark_gray> <gray>Stock: <aqua>" + (shopItem.isInfiniteStock() ? "Infinito" : shopItem.getStock()) + "</aqua>"));
        lore.add(MessageUtils.parse("<dark_gray>»</dark_gray> <gray>Cooldown: <yellow>" + formatTime(shopItem.getCooldown()) + "</yellow>"));
        lore.add(MessageUtils.parse(" "));
        lore.add(MessageUtils.parse("<gray><italic>Click Izq: Editar | Click Der: Eliminar</italic></gray>"));
        
        meta.getPersistentDataContainer().set(shopItemIdKey, PersistentDataType.INTEGER, originalSlot);
        
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack createPlayerDisplayItem(ShopItem shopItem, PlayerData playerData, int slot) {
        ItemStack display = shopItem.getItemStack().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(display.getType());
        }

        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(MessageUtils.parse(" "));
        lore.add(MessageUtils.parse("<gray>--------------------</gray>"));

        if (!shopItem.isInfiniteStock()) {
            String stockLine = plugin.getLangManager().getMessage("shop.item.stock_format")
                    .replace("%stock%", String.valueOf(shopItem.getStock()));
            lore.add(MessageUtils.parse(stockLine));
        }

        if (shopItem.getPrice() > 0) {
            String priceLine = plugin.getLangManager().getMessage("shop.item.price_format")
                    .replace("%price%", String.valueOf(shopItem.getPrice()));
            lore.add(MessageUtils.parse(priceLine));
        } else {
            lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("shop.item.price_free")));
        }
        lore.add(" ");

        long remainingCooldown = 0;
        if (shopItem.getCooldown() > 0) {
            long lastPurchase = playerData.getPurchasedShopItems().getOrDefault(String.valueOf(slot), 0L);
            long timeSince = (System.currentTimeMillis() - lastPurchase) / 1000;
            if (timeSince < shopItem.getCooldown()) {
                remainingCooldown = shopItem.getCooldown() - timeSince;
            }
        }

        boolean canAfford = playerData.getContractPoints() >= shopItem.getPrice();
        boolean hasStock = shopItem.isInfiniteStock() || shopItem.getStock() > 0;

        if (remainingCooldown > 0) {
            String cooldownMessage = plugin.getLangManager().getMessage("shop.item.status_on_cooldown")
                    .replace("%time%", formatTime(remainingCooldown));
            lore.add(MessageUtils.parse(cooldownMessage));
        } else if (!hasStock) {
            lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("shop.item.status_no_stock")));
        } else if (!canAfford) {
            lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("shop.item.status_no_funds")));
        } else {
            lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("shop.item.status_buy")));
        }

        lore.add(MessageUtils.parse("<gray>--------------------</gray>"));

        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private void fillBorders(Inventory gui, ItemStack pane) {
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        int size = gui.getSize();
        for (int i = 0; i < size; i++) {
            if (gui.getItem(i) == null || gui.getItem(i).getType() == Material.AIR) {
                 if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                    gui.setItem(i, pane);
                }
            }
        }
    }
    
    public static String formatTime(long seconds) {
        if (seconds <= 0) return "N/A";
        long days = TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        long secs = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("s");
        return sb.toString().trim();
    }
}