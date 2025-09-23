package com.infinity3113.contractrpg.shop;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.data.PlayerData;
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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopListener implements Listener {

    private final ContractRPG plugin;
    private final Map<UUID, ChatInputSession> chatInputTasks = new HashMap<>();

    private static class ChatInputSession {
        final String task;
        final int slot;

        ChatInputSession(String task, int slot) {
            this.task = task;
            this.slot = slot;
        }
    }

    public ShopListener(ContractRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof ShopGUI.ShopEditorHolder) {
            handlePagination(event, holder);
            handleMainEditorClick(event);
        } else if (holder instanceof ShopGUI.ItemEditorHolder) {
            handleItemEditorClick(event, (ShopGUI.ItemEditorHolder) holder);
        } else if (holder instanceof ShopGUI.PlayerShopHolder) {
            handlePagination(event, holder);
            handlePlayerShopClick(event);
        }
    }

    private void handlePagination(InventoryClickEvent event, InventoryHolder holder) {
        if (event.getSlot() != 45 && event.getSlot() != 53) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int currentPage = 0;
        boolean isEditor = holder instanceof ShopGUI.ShopEditorHolder;

        if (isEditor) {
            currentPage = ((ShopGUI.ShopEditorHolder) holder).getPage();
        } else {
            currentPage = ((ShopGUI.PlayerShopHolder) holder).getPage();
        }

        if (event.getSlot() == 45) { // Página Anterior
            if (isEditor) plugin.getShopGUI().openEditor(player, currentPage - 1);
            else plugin.getShopGUI().openShop(player, currentPage - 1);
        } else if (event.getSlot() == 53) { // Página Siguiente
            if (isEditor) plugin.getShopGUI().openEditor(player, currentPage + 1);
            else plugin.getShopGUI().openShop(player, currentPage + 1);
        }
    }

    private void handlePlayerShopClick(InventoryClickEvent event) {
        if (event.getSlot() >= 45) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

        Player player = (Player) event.getWhoClicked();
        
        List<Map.Entry<Integer, ShopItem>> sortedItems = new ArrayList<>(plugin.getShopManager().getShopItems().entrySet());
        sortedItems.sort(Map.Entry.comparingByKey());
        
        int page = ((ShopGUI.PlayerShopHolder) event.getInventory().getHolder()).getPage();
        int index = (page * 45) + event.getSlot();
        if (index >= sortedItems.size()) return;
        
        int originalSlot = sortedItems.get(index).getKey();
        ShopItem shopItem = plugin.getShopManager().getItem(originalSlot);
        if (shopItem == null) return;

        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData == null) return;

        if (shopItem.getCooldown() > 0) {
            long lastPurchase = playerData.getPurchasedShopItems().getOrDefault(String.valueOf(originalSlot), 0L);
            long timeSince = (System.currentTimeMillis() - lastPurchase) / 1000;
            if (timeSince < shopItem.getCooldown()) {
                MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.error.on-cooldown"));
                return;
            }
        }
        if (playerData.getContractPoints() < shopItem.getPrice()) {
            MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.error.no-funds"));
            return;
        }
        if (!shopItem.isInfiniteStock() && shopItem.getStock() <= 0) {
            MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.error.no-stock"));
            return;
        }

        playerData.setContractPoints(playerData.getContractPoints() - (int) shopItem.getPrice());
        if (!shopItem.isInfiniteStock()) {
            shopItem.setStock(shopItem.getStock() - 1);
        }
        playerData.addPurchasedShopItem(String.valueOf(originalSlot));
        plugin.getShopManager().saveItems();
        
        player.getInventory().addItem(shopItem.getItemStack().clone());
        MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.success.purchase")
                .replace("%item%", shopItem.getItemStack().hasItemMeta() && shopItem.getItemStack().getItemMeta().hasDisplayName() ? shopItem.getItemStack().getItemMeta().getDisplayName() : shopItem.getItemStack().getType().name()));

        plugin.getShopGUI().openShop(player, page);
    }

    private void handleMainEditorClick(InventoryClickEvent event) {
        if (event.getSlot() >= 45 && event.getSlot() <= 53) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) return;

        event.setCancelled(true);
        ItemStack cursorItem = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
        int page = ((ShopGUI.ShopEditorHolder) event.getInventory().getHolder()).getPage();
        
        int newSlot = plugin.getShopManager().getShopItems().keySet().stream().max(Integer::compare).orElse(-1) + 1;

        if (cursorItem != null && cursorItem.getType() != Material.AIR && (currentItem == null || currentItem.getType() == Material.AIR)) {
            plugin.getShopManager().setItem(newSlot, new ShopItem(cursorItem.clone(), 0, 1, false, 0));
            cursorItem.setAmount(0);
            plugin.getShopGUI().openEditor(player, page);
            return;
        }

        if (currentItem == null || currentItem.getType().isAir()) return;

        List<Map.Entry<Integer, ShopItem>> sortedItems = new ArrayList<>(plugin.getShopManager().getShopItems().entrySet());
        sortedItems.sort(Map.Entry.comparingByKey());
        int index = (page * 45) + event.getSlot();
        if (index >= sortedItems.size()) return;

        int originalSlot = sortedItems.get(index).getKey();
        ShopItem shopItem = plugin.getShopManager().getItem(originalSlot);

        if (shopItem == null) return;

        if (event.getClick() == ClickType.LEFT) {
            plugin.getShopGUI().openItemEditor(player, originalSlot, shopItem);
        } else if (event.getClick() == ClickType.RIGHT) {
            plugin.getShopManager().setItem(originalSlot, null);
            plugin.getShopGUI().openEditor(player, page);
        }
    }

    private void handleItemEditorClick(InventoryClickEvent event, ShopGUI.ItemEditorHolder holder) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = holder.getSlot();

        ShopItem shopItem = plugin.getShopManager().getItem(slot);
        if (shopItem == null) {
            player.closeInventory();
            MessageUtils.sendMessage(player, "<red>Este item ya no existe.</red>");
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (clicked.getType()) {
            case GOLD_INGOT:
                startChatInput(player, "price", slot);
                break;
            case CLOCK:
                startChatInput(player, "cooldown", slot);
                break;
            case CHEST:
                int amount = (event.getClick() == ClickType.LEFT) ? 1 : -1;
                if (event.isShiftClick()) amount *= 10;
                if (event.getClick() == ClickType.MIDDLE) {
                    shopItem.setInfiniteStock(!shopItem.isInfiniteStock());
                } else if (!shopItem.isInfiniteStock()) {
                    shopItem.setStock(Math.max(0, shopItem.getStock() + amount));
                }
                plugin.getShopManager().setItem(slot, shopItem);
                plugin.getShopGUI().openItemEditor(player, slot, shopItem);
                break;
            case BARRIER:
                int page = plugin.getShopGUI().editorPages.getOrDefault(player.getUniqueId(), 0);
                plugin.getShopGUI().openEditor(player, page);
                break;
        }
    }

    private void startChatInput(Player player, String task, int slot) {
        chatInputTasks.put(player.getUniqueId(), new ChatInputSession(task, slot));
        player.closeInventory();
        MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.editor.prompt-" + task));
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
            int slot = session.slot;
            ShopItem shopItem = plugin.getShopManager().getItem(slot);
            if (shopItem == null) {
                MessageUtils.sendMessage(player, "<red>Error: El ítem que intentabas editar ya no existe.</red>");
                return;
            }

            if (message.equalsIgnoreCase("cancelar")) {
                MessageUtils.sendMessage(player, "<red>Edición cancelada.</red>");
                plugin.getShopGUI().openItemEditor(player, slot, shopItem);
                return;
            }

            try {
                switch (session.task) {
                    case "price":
                        shopItem.setPrice(Math.max(0, Double.parseDouble(message)));
                        break;
                    case "cooldown":
                        shopItem.setCooldown(Math.max(0, Long.parseLong(message)));
                        break;
                }
                plugin.getShopManager().setItem(slot, shopItem);
                MessageUtils.sendMessage(player, "<green>¡Valor actualizado con éxito!</green>");
            } catch (NumberFormatException e) {
                MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.editor.error-invalid-number"));
            }

            plugin.getShopGUI().openItemEditor(player, slot, shopItem);
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getShopGUI().playerShopPages.remove(uuid);
        plugin.getShopGUI().editorPages.remove(uuid);
    }
}