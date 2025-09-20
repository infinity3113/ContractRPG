package com.infinity3113.contractrpg.shop;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ShopListener implements Listener {
    private final ContractRPG plugin;
    private final NamespacedKey shopItemIdKey;

    public ShopListener(ContractRPG plugin) {
        this.plugin = plugin;
        this.shopItemIdKey = new NamespacedKey(plugin, "shop-item-id");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String shopTitle = MessageUtils.parse(plugin.getShopManager().getConfig().getString("shop-title", "<bold>Tienda</bold>"));
        String editorTitle = MessageUtils.parse("<bold>Editor de Tienda</bold>");
        String viewTitle = event.getView().getTitle();

        if (!viewTitle.equals(shopTitle) && !viewTitle.equals(editorTitle)) return;
        
        Player player = (Player) event.getWhoClicked();

        if (viewTitle.equals(shopTitle)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
            handleShopClick(player, event.getCurrentItem());
            return;
        }

        if (viewTitle.equals(editorTitle)) {
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                return;
            }

            if (event.getClick() == ClickType.LEFT && event.getCursor() != null && !event.getCursor().getType().isAir() && event.getCurrentItem() == null) {
                event.setCancelled(true);
                handleEditorAddItem(player, event.getCursor(), event.getSlot());
                return;
            }

            if (event.getClick() == ClickType.SHIFT_LEFT && event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
                 if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(shopItemIdKey, PersistentDataType.STRING)) {
                    event.setCancelled(true);
                    handleEditorRemoveItem(player, event.getCurrentItem());
                    return;
                }
            }
            event.setCancelled(true);
        }
    }

    private void handleEditorAddItem(Player player, ItemStack cursorItem, int slot) {
        ItemStack itemToAdd = cursorItem.clone();
        itemToAdd.setAmount(1);
        String uniqueId = "item_" + System.currentTimeMillis();

        plugin.getShopManager().adminEditing.put(player.getUniqueId(), uniqueId + ";" + slot);
        player.setMetadata("shop_item_to_add", new org.bukkit.metadata.FixedMetadataValue(plugin, itemToAdd));

        player.closeInventory();
        MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.editor.prompt-price"));
    }

    private void handleEditorRemoveItem(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(shopItemIdKey, PersistentDataType.STRING)) return;
        
        String itemId = meta.getPersistentDataContainer().get(shopItemIdKey, PersistentDataType.STRING);
        if (itemId != null) {
            plugin.getShopManager().removeItem(itemId);
            MessageUtils.sendMessage(player, "<green>Ítem eliminado. Re-abriendo editor...");
            
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getShopGUI().openEditor(player));
        }
    }

    private void handleShopClick(Player player, ItemStack clickedItem) {
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().getPersistentDataContainer().has(shopItemIdKey, PersistentDataType.STRING)) return;

        String itemId = clickedItem.getItemMeta().getPersistentDataContainer().get(shopItemIdKey, PersistentDataType.STRING);
        ConfigurationSection itemSection = plugin.getShopManager().getItemsSection().getConfigurationSection(itemId);
        if (itemSection == null) return;

        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        int price = itemSection.getInt("price");
        long stockResetSeconds = itemSection.getLong("stock-reset-seconds");

        if (playerData.getPurchasedShopItems().containsKey(itemId)) {
            long purchaseTime = playerData.getPurchasedShopItems().get(itemId);
            long cooldownMillis = TimeUnit.SECONDS.toMillis(stockResetSeconds);
            if (System.currentTimeMillis() - purchaseTime < cooldownMillis) {
                MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.error.on-cooldown"));
                return;
            }
        }

        if (playerData.getContractPoints() < price) {
            MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.error.no-funds"));
            return;
        }

        playerData.setContractPoints(playerData.getContractPoints() - price);
        playerData.addPurchasedShopItem(itemId);
        
        ItemStack purchasedItem = ItemSerializer.deserialize(itemSection.getString("item"));
        player.getInventory().addItem(purchasedItem);
        MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.success.purchase").replace("%item%", purchasedItem.getItemMeta().getDisplayName()));
        player.closeInventory();
    }
    
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (plugin.getShopManager().adminEditing.containsKey(uuid)) {
            event.setCancelled(true);
            String message = event.getMessage();

            // Ejecutar la lógica en el hilo principal del servidor para evitar problemas
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!plugin.getShopManager().adminEditing.containsKey(uuid)) return;

                if (message.equalsIgnoreCase("cancelar")) {
                    plugin.getShopManager().adminEditing.remove(uuid);
                    player.removeMetadata("shop_price_set", plugin);
                    player.removeMetadata("shop_item_to_add", plugin);
                    MessageUtils.sendMessage(player, "<red>Operación cancelada.");
                    return;
                }

                String[] editData = plugin.getShopManager().adminEditing.get(uuid).split(";");
                String itemId = editData[0];
                int slot = Integer.parseInt(editData[1]);

                if (!player.hasMetadata("shop_price_set")) {
                    try {
                        int price = Integer.parseInt(message);
                        player.setMetadata("shop_price_set", new org.bukkit.metadata.FixedMetadataValue(plugin, price));
                        MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.editor.prompt-cooldown"));
                    } catch (NumberFormatException e) {
                        MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.editor.error-invalid-number"));
                    }
                    return;
                }

                if (player.hasMetadata("shop_price_set")) {
                    try {
                        int cooldown = Integer.parseInt(message);
                        int price = (int) player.getMetadata("shop_price_set").get(0).value();
                        ItemStack item = (ItemStack) player.getMetadata("shop_item_to_add").get(0).value();

                        plugin.getShopManager().saveItem(itemId, item, price, cooldown, slot);
                        MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.editor.success"));

                        plugin.getShopManager().adminEditing.remove(uuid);
                        player.removeMetadata("shop_price_set", plugin);
                        player.removeMetadata("shop_item_to_add", plugin);

                    } catch (NumberFormatException e) {
                        MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.editor.error-invalid-number"));
                    }
                }
            });
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (plugin.getShopManager().adminEditing.containsKey(player.getUniqueId()) && !player.hasMetadata("shop_price_set")) {
            plugin.getShopManager().adminEditing.remove(player.getUniqueId());
            player.removeMetadata("shop_price_set", plugin);
            player.removeMetadata("shop_item_to_add", plugin);
        }
    }
}