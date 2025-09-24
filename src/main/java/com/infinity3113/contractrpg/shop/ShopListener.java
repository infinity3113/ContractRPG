package com.infinity3113.contractrpg.shop;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.managers.LangManager;
import com.infinity3113.contractrpg.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ShopListener implements Listener {

    private final ContractRPG plugin;
    private final LangManager langManager;
    private final Map<UUID, ChatInputSession> chatInputTasks = new HashMap<>();

    // Clase interna para manejar las entradas de chat
    private static class ChatInputSession {
        final String task;
        final int slotId;
        final ItemStack itemToAdd;

        ChatInputSession(String task, int slotId) {
            this.task = task;
            this.slotId = slotId;
            this.itemToAdd = null;
        }

        ChatInputSession(String task, ItemStack itemToAdd) {
            this.task = task;
            this.itemToAdd = itemToAdd;
            this.slotId = -1;
        }
    }

    public ShopListener(ContractRPG plugin) {
        this.plugin = plugin;
        this.langManager = plugin.getLangManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getHolder() == null) return;

        InventoryHolder holder = topInventory.getHolder();

        if (holder instanceof ShopGUI.ShopEditorHolder) {
            handleMainEditorClick(event, player);
        } else if (holder instanceof ShopGUI.ItemEditorHolder) {
            handleItemEditorClick(event, player, (ShopGUI.ItemEditorHolder) holder);
        } else if (holder instanceof ShopGUI.PlayerShopHolder) {
            handlePlayerShopClick(event, player, (ShopGUI.PlayerShopHolder) holder);
        } else if (holder instanceof ShopGUI.AddItemSelectionHolder) {
            handleAddItemSelectionClick(event, player);
        }
    }

    private void handleMainEditorClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ShopGUI.ShopEditorHolder holder = (ShopGUI.ShopEditorHolder) event.getView().getTopInventory().getHolder();
        int currentPage = holder.getPage();
        int clickedSlot = event.getSlot();

        if (clickedSlot == 45 || clickedSlot == 53) {
            int newPage = clickedSlot == 45 ? currentPage - 1 : currentPage + 1;
            plugin.getShopGUI().openEditor(player, newPage);
            return;
        }

        if (clickedSlot == 49) {
            plugin.getShopGUI().openAddItemSelection(player);
            return;
        }

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && !currentItem.getType().isAir()) {
            ItemMeta meta = currentItem.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(plugin.getShopGUI().shopItemIdKey, PersistentDataType.INTEGER)) {
                Integer slotId = meta.getPersistentDataContainer().get(plugin.getShopGUI().shopItemIdKey, PersistentDataType.INTEGER);
                if (slotId != null) {
                    if (event.getClick() == ClickType.LEFT) {
                        plugin.getShopGUI().openItemEditor(player, slotId);
                    } else if (event.getClick() == ClickType.RIGHT) {
                        plugin.getShopManager().removeItemAndSave(slotId);
                        event.getClickedInventory().setItem(clickedSlot, null);
                    }
                }
            }
        }
    }

    private void handleAddItemSelectionClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem != null && event.getSlot() == 49) {
            plugin.getShopGUI().openEditor(player, 0);
            return;
        }

        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            player.closeInventory();
            MessageUtils.sendMessage(player, langManager.getMessage("shop.editor.prompt-add"));
            chatInputTasks.put(player.getUniqueId(), new ChatInputSession("add", clickedItem.clone()));
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!chatInputTasks.containsKey(uuid)) return;

        event.setCancelled(true);
        ChatInputSession session = chatInputTasks.remove(uuid);
        String message = event.getMessage();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (message.equalsIgnoreCase("cancelar")) {
                MessageUtils.sendMessage(player, "<red>Acción cancelada.</red>");
                if (session.task.equals("add")) {
                    plugin.getShopGUI().openEditor(player, 0);
                } else {
                    plugin.getShopGUI().openItemEditor(player, session.slotId);
                }
                return;
            }

            if (session.task.equals("add")) {
                handleAddItemViaChat(player, session, message);
                return;
            }

            ShopItem shopItem = plugin.getShopManager().getItem(session.slotId);
            if (shopItem == null) {
                MessageUtils.sendMessage(player, "<red>Error: El ítem que intentabas editar ya no existe.</red>");
                return;
            }
            try {
                if (session.task.equals("price")) {
                    shopItem.setPrice(Math.max(0, Double.parseDouble(message)));
                } else if (session.task.equals("cooldown")) {
                    shopItem.setCooldown(Math.max(0, Long.parseLong(message)));
                }
                plugin.getShopManager().setItemAndSave(session.slotId, shopItem);
                MessageUtils.sendMessage(player, "<green>¡Valor actualizado con éxito!</green>");
            } catch (NumberFormatException e) {
                MessageUtils.sendMessage(player, langManager.getMessage("shop.editor.error-invalid-number"));
            }
            plugin.getShopGUI().openItemEditor(player, session.slotId);
        });
    }

    private void handleAddItemViaChat(Player player, ChatInputSession session, String message) {
        String[] parts = message.split(" ");
        if (parts.length == 0) {
            MessageUtils.sendMessage(player, "<red>Formato incorrecto. Uso: <precio> [stock]");
            plugin.getShopGUI().openEditor(player, 0);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(parts[0]);
        } catch (NumberFormatException e) {
            MessageUtils.sendMessage(player, "<red>El precio debe ser un número válido.");
            plugin.getShopGUI().openEditor(player, 0);
            return;
        }

        int stock = -1;
        boolean infiniteStock = true;
        if (parts.length > 1) {
            try {
                stock = Integer.parseInt(parts[1]);
                infiniteStock = false;
            } catch (NumberFormatException e) {
                MessageUtils.sendMessage(player, "<red>El stock debe ser un número entero válido.");
                plugin.getShopGUI().openEditor(player, 0);
                return;
            }
        }

        int newSlotId = plugin.getShopManager().findNextFreeSlot();
        ShopItem newShopItem = new ShopItem(session.itemToAdd, price, stock, infiniteStock, 0);
        plugin.getShopManager().setItemAndSave(newSlotId, newShopItem);

        MessageUtils.sendMessage(player, "<green>¡Ítem añadido a la tienda con ID " + newSlotId + "!");
        plugin.getShopGUI().openEditor(player, 0);
    }

    private void handleItemEditorClick(InventoryClickEvent event, Player player, ShopGUI.ItemEditorHolder holder) {
        event.setCancelled(true);
        int slotId = holder.getSlotId();
        ShopItem shopItem = plugin.getShopManager().getItem(slotId);
        if (shopItem == null) {
            player.closeInventory();
            MessageUtils.sendMessage(player, "<red>Este ítem ya no existe.</red>");
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        switch (clicked.getType()) {
            case GOLD_INGOT:
                startChatInput(player, "price", slotId);
                break;
            case CLOCK:
                startChatInput(player, "cooldown", slotId);
                break;
            case CHEST:
                handleStockChange(event, shopItem);
                plugin.getShopManager().setItemAndSave(slotId, shopItem);
                plugin.getShopGUI().openItemEditor(player, slotId);
                break;
            case BARRIER:
                int page = plugin.getShopGUI().editorPages.getOrDefault(player.getUniqueId(), 0);
                plugin.getShopGUI().openEditor(player, page);
                break;
        }
    }

    private void startChatInput(Player player, String task, int slotId) {
        chatInputTasks.put(player.getUniqueId(), new ChatInputSession(task, slotId));
        player.closeInventory();
        MessageUtils.sendMessage(player, langManager.getMessage("shop.editor.prompt-" + task));
    }

    private void handleStockChange(InventoryClickEvent event, ShopItem item) {
        if (event.getClick() == ClickType.MIDDLE) {
            item.setInfiniteStock(!item.isInfiniteStock());
            return;
        }
        if (item.isInfiniteStock()) return;
        int amount = (event.getClick() == ClickType.LEFT) ? 1 : -1;
        if (event.isShiftClick()) amount *= 10;
        item.setStock(Math.max(0, item.getStock() + amount));
    }

    private void handlePlayerShopClick(InventoryClickEvent event, Player player, ShopGUI.PlayerShopHolder holder) {
        event.setCancelled(true);
        int currentPage = holder.getPage();
        if (event.getSlot() == 45 || event.getSlot() == 53) {
            int newPage = event.getSlot() == 45 ? currentPage - 1 : currentPage + 1;
            plugin.getShopGUI().openShop(player, newPage);
            return;
        }
        if (event.getSlot() >= 45 || event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
        List<Map.Entry<Integer, ShopItem>> sortedItems = new ArrayList<>(plugin.getShopManager().getShopItems().entrySet());
        sortedItems.sort(Map.Entry.comparingByKey());
        int index = (currentPage * 45) + event.getSlot();
        if (index >= sortedItems.size()) return;
        int slotId = sortedItems.get(index).getKey();
        ShopItem shopItem = plugin.getShopManager().getItem(slotId);
        if (shopItem == null) return;
        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData == null) return;
        if (shopItem.getPrice() > playerData.getContractPoints()) {
            MessageUtils.sendMessage(player, langManager.getMessage("shop.error.no-funds"));
            return;
        }
        if (!shopItem.isInfiniteStock() && shopItem.getStock() <= 0) {
            MessageUtils.sendMessage(player, langManager.getMessage("shop.error.no-stock"));
            return;
        }
        long lastPurchase = playerData.getPurchasedShopItems().getOrDefault(String.valueOf(slotId), 0L);
        if (System.currentTimeMillis() - lastPurchase < shopItem.getCooldown() * 1000) {
            MessageUtils.sendMessage(player, langManager.getMessage("shop.error.on-cooldown"));
            return;
        }
        playerData.setContractPoints(playerData.getContractPoints() - (int) shopItem.getPrice());
        if (!shopItem.isInfiniteStock()) {
            shopItem.setStock(shopItem.getStock() - 1);
        }
        playerData.addPurchasedShopItem(String.valueOf(slotId));
        plugin.getShopManager().setItemAndSave(slotId, shopItem);
        player.getInventory().addItem(shopItem.getItemStack().clone());
        MessageUtils.sendMessage(player, langManager.getMessage("shop.success.purchase")
                .replace("%item%", shopItem.getItemStack().hasItemMeta() && shopItem.getItemStack().getItemMeta().hasDisplayName() ? shopItem.getItemStack().getItemMeta().getDisplayName() : shopItem.getItemStack().getType().name()));
        plugin.getShopGUI().openShop(player, currentPage);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // <-- LÓGICA PROBLEMÁTICA ELIMINADA -->
        // Ya no limpiamos la tarea de chat aquí.
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getShopGUI().playerShopPages.remove(uuid);
        plugin.getShopGUI().editorPages.remove(uuid);
    }
}