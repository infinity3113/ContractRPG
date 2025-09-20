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
            handleMainEditorClick(event);
        } else if (holder instanceof ShopGUI.ItemEditorHolder) {
            handleItemEditorClick(event, (ShopGUI.ItemEditorHolder) holder);
        } else if (holder instanceof ShopGUI.PlayerShopHolder) {
            handlePlayerShopClick(event);
        }
    }

    private void handlePlayerShopClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        ShopItem shopItem = plugin.getShopManager().getItem(slot);
        if (shopItem == null) return;

        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData == null) return;

        if (shopItem.getCooldown() > 0) {
            long lastPurchase = playerData.getPurchasedShopItems().getOrDefault(String.valueOf(slot), 0L);
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
        playerData.addPurchasedShopItem(String.valueOf(slot));
        plugin.getShopManager().saveItems();

        if (shopItem.getCommands() != null) {
            for (String command : shopItem.getCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
            }
        }
        player.getInventory().addItem(shopItem.getItemStack().clone());

        MessageUtils.sendMessage(player, plugin.getLangManager().getMessage("shop.success.purchase")
                .replace("%item%", shopItem.getItemStack().hasItemMeta() && shopItem.getItemStack().getItemMeta().hasDisplayName() ? shopItem.getItemStack().getItemMeta().getDisplayName() : shopItem.getItemStack().getType().name()));

        plugin.getShopGUI().openShop(player);
    }

    private void handleMainEditorClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) return;

        event.setCancelled(true);
        ItemStack cursorItem = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();

        if (cursorItem != null && cursorItem.getType() != Material.AIR && (currentItem == null || currentItem.getType() == Material.AIR)) {
            // ===== CORREGIDO: Inicializar la lista de comandos vacía en lugar de nula =====
            plugin.getShopManager().setItem(event.getSlot(), new ShopItem(cursorItem.clone(), 0, 1, false, 0, new ArrayList<>()));
            cursorItem.setAmount(0);
            plugin.getShopGUI().openEditor(player);
            return;
        }

        if (currentItem == null || currentItem.getType() == Material.AIR) return;
        
        // El item de ayuda no debe ser interactuable
        if (currentItem.getType() == Material.BOOK && event.getSlot() == event.getInventory().getSize() -1) return;

        ShopItem shopItem = plugin.getShopManager().getItem(event.getSlot());
        if (shopItem == null) return;

        if (event.getClick() == ClickType.LEFT) {
            plugin.getShopGUI().openItemEditor(player, event.getSlot(), shopItem);
        } else if (event.getClick() == ClickType.RIGHT) {
            plugin.getShopManager().setItem(event.getSlot(), null);
            plugin.getShopGUI().openEditor(player);
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
            case COMMAND_BLOCK:
                if (event.getClick() == ClickType.LEFT) {
                    startChatInput(player, "command", slot);
                } else if (event.getClick() == ClickType.RIGHT) {
                    shopItem.setCommands(new ArrayList<>());
                    plugin.getShopManager().setItem(slot, shopItem);
                    plugin.getShopGUI().openItemEditor(player, slot, shopItem);
                }
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
                plugin.getShopGUI().openEditor(player);
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
                    case "command":
                        List<String> commands = shopItem.getCommands();
                        if (commands == null) {
                           commands = new ArrayList<>();
                        }
                        commands.add(message);
                        shopItem.setCommands(commands);
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
}