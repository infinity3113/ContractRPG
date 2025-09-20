package com.infinity3113.contractrpg.shop;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ShopGUI {

    private final ContractRPG plugin;

    public ShopGUI(ContractRPG plugin) {
        this.plugin = plugin;
    }
    
    // ===== INVENTORY HOLDERS (LA SOLUCIÓN AL BUG) =====
    public static class PlayerShopHolder implements InventoryHolder {
        @Override public @NotNull Inventory getInventory() { return null; }
    }
    public static class ShopEditorHolder implements InventoryHolder {
        @Override public @NotNull Inventory getInventory() { return null; }
    }
    public static class ItemEditorHolder implements InventoryHolder {
        private final int slot;
        public ItemEditorHolder(int slot) { this.slot = slot; }
        public int getSlot() { return slot; }
        @Override public @NotNull Inventory getInventory() { return null; }
    }


    public void openShop(Player player) {
        String title = MessageUtils.parse(plugin.getShopManager().getConfig().getString("shop-title", "<bold><#F52E27>Tienda de Contratos</bold>"));
        int size = plugin.getShopManager().getConfig().getInt("shop-size", 54);
        Inventory shop = Bukkit.createInventory(new PlayerShopHolder(), size, title);
        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData == null) return;

        for (Map.Entry<Integer, ShopItem> entry : plugin.getShopManager().getShopItems().entrySet()) {
            if (entry.getKey() < size) {
                shop.setItem(entry.getKey(), createPlayerDisplayItem(entry.getValue(), playerData, entry.getKey()));
            }
        }
        player.openInventory(shop);
    }

    public void openEditor(Player player) {
        String title = MessageUtils.parse(plugin.getShopManager().getConfig().getString("editor-title", "<bold><#27A6F5>Editor de la Tienda</bold>"));
        int size = plugin.getShopManager().getConfig().getInt("shop-size", 54);
        Inventory editor = Bukkit.createInventory(new ShopEditorHolder(), size, title);

        for (Map.Entry<Integer, ShopItem> entry : plugin.getShopManager().getShopItems().entrySet()) {
            if (entry.getKey() < size) {
                editor.setItem(entry.getKey(), createEditorDisplayItem(entry.getValue()));
            }
        }
        fillBorders(editor, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        player.openInventory(editor);
    }

    public void openItemEditor(Player player, int slot, ShopItem shopItem) {
        Inventory itemEditor = Bukkit.createInventory(new ItemEditorHolder(slot), 36, MessageUtils.parse("<dark_gray>Editando Item"));

        itemEditor.setItem(4, shopItem.getItemStack());

        itemEditor.setItem(19, createButton(Material.GOLD_INGOT, "<yellow><bold>Editar Precio</bold></yellow>", Arrays.asList(
                "<gray>Precio actual: <gold>" + shopItem.getPrice() + "</gold>",
                "<white>Click para cambiar."
        )));

        itemEditor.setItem(21, createButton(Material.CHEST, "<aqua><bold>Editar Stock</bold></aqua>", Arrays.asList(
                "<gray>Stock: <blue>" + (shopItem.isInfiniteStock() ? "Infinito" : shopItem.getStock()) + "</blue>",
                "<white>Click IZQ: +1 | Click DER: -1",
                "<white>SHIFT + Click: +/- 10",
                "<white>Click MEDIO: Infinito"
        )));

        itemEditor.setItem(23, createButton(Material.CLOCK, "<#FF8C00><bold>Editar Reabastecimiento</bold></#FF8C00>", Arrays.asList(
                "<gray>Cooldown: <yellow>" + formatTime(shopItem.getCooldown()) + "</yellow>",
                "<white>Click para cambiar (en segundos)."
        )));

        // ===== ¡NUEVO! Botón para editar comandos =====
        List<String> commandLore = new ArrayList<>();
        commandLore.add("<gray>Comandos a ejecutar:");
        if (shopItem.getCommands().isEmpty()) {
            commandLore.add("<gray><italic>Ninguno</italic>");
        } else {
            shopItem.getCommands().forEach(cmd -> commandLore.add("<#ADD8E6>- " + cmd));
        }
        commandLore.add(" ");
        commandLore.add("<white>Click IZQ para AÑADIR un comando.");
        commandLore.add("<white>Click DER para LIMPIAR los comandos.");
        itemEditor.setItem(25, createButton(Material.COMMAND_BLOCK, "<#32CD32><bold>Editar Comandos</bold></#32CD32>", commandLore));


        itemEditor.setItem(31, createButton(Material.BARRIER, "<red><bold>Volver</bold></red>", null));

        fillBorders(itemEditor, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        player.openInventory(itemEditor);
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

    private ItemStack createEditorDisplayItem(ShopItem shopItem) {
        ItemStack display = shopItem.getItemStack().clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(MessageUtils.parse(" "));
        lore.add(MessageUtils.parse("<gray>--------------------</gray>"));
        lore.add(MessageUtils.parse("<yellow><bold>DATOS DE LA TIENDA</bold></yellow>"));
        lore.add(MessageUtils.parse("<gray>Precio: <gold>" + shopItem.getPrice() + " Puntos</gold>"));
        lore.add(MessageUtils.parse("<gray>Stock: <aqua>" + (shopItem.isInfiniteStock() ? "Infinito" : shopItem.getStock()) + "</aqua>"));
        lore.add(MessageUtils.parse("<gray>Cooldown: <yellow>" + formatTime(shopItem.getCooldown()) + "</yellow>"));
        lore.add(MessageUtils.parse("<gray>--------------------</gray>"));
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack createPlayerDisplayItem(ShopItem shopItem, PlayerData playerData, int slot) {
        ItemStack display = shopItem.getItemStack().clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(MessageUtils.parse(" "));

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

        if (canAfford && hasStock && remainingCooldown == 0) {
            lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("shop.item.buyable").replace("%price%", String.valueOf(shopItem.getPrice()))));
        } else {
            lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("shop.item.no-funds").replace("%price%", String.valueOf(shopItem.getPrice()))));
        }

        if (remainingCooldown > 0) {
            lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("shop.item.cooldown").replace("%time%", formatTime(remainingCooldown))));
        }

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