package com.infinity3113.contractrpg.shop;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.util.MessageUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopListener implements Listener {

    private final ContractRPG plugin;
    private final Map<UUID, Integer> editingPrice = new HashMap<>();

    public ShopListener(ContractRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String viewTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (viewTitle.contains("Editor de la Tienda")) {
            handleMainEditorClick(event);
        } else if (viewTitle.contains("Editando Item")) {
            handleItemEditorClick(event);
        }
    }

    private void handleMainEditorClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack cursorItem = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();

        if (event.getClickedInventory() == null) return;

        // Si el jugador hace click en su propio inventario, lo permitimos y no hacemos nada más.
        if (event.getClickedInventory().equals(player.getInventory())) {
            return;
        }

        // --- LÓGICA PARA AÑADIR UN ITEM (DRAG & DROP) ---
        // Si el jugador tiene un item en el cursor y hace click en un slot vacío del editor.
        if (cursorItem != null && cursorItem.getType() != Material.AIR && (currentItem == null || currentItem.getType() == Material.AIR)) {
            // No cancelamos el evento. Bukkit se encargará de colocar el item.
            ShopItem newShopItem = new ShopItem(cursorItem.clone(), 0, 1, false);
            plugin.getShopManager().setItem(event.getSlot(), newShopItem);

            // Forzamos una actualización de la GUI en el siguiente tick para que el lore se muestre.
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.getShopGUI().openEditor(player), 1L);
            return; // Salimos de la función para no cancelar el evento.
        }

        // Si no estamos añadiendo un item, cancelamos todos los demás tipos de clicks en el editor.
        event.setCancelled(true);

        if (currentItem == null || currentItem.getType() == Material.AIR) return;

        // Evitar interacciones con el item de información.
        if (event.getSlot() == event.getInventory().getSize() - 1) return;

        ShopItem shopItem = plugin.getShopManager().getItem(event.getSlot());
        if (shopItem == null) return;

        // Editar item (Click Izquierdo)
        if (event.getClick() == ClickType.LEFT) {
            plugin.getShopGUI().openItemEditor(player, event.getSlot(), shopItem);
        }

        // Eliminar item (Click Derecho)
        if (event.getClick() == ClickType.RIGHT) {
            plugin.getShopManager().setItem(event.getSlot(), null);
            plugin.getShopGUI().openEditor(player);
        }
    }

    private void handleItemEditorClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        int slot;
        try {
            slot = Integer.parseInt(title.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return; // No se pudo parsear el slot, salir.
        }

        ShopItem shopItem = plugin.getShopManager().getItem(slot);
        if (shopItem == null) {
            player.closeInventory();
            MessageUtils.sendMessage(player, "<red>Este item ya no existe en la tienda.</red>");
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (clicked.getType()) {
            case GOLD_INGOT:
                editingPrice.put(player.getUniqueId(), slot);
                player.closeInventory();
                MessageUtils.sendMessage(player, "<gold>Escribe el nuevo precio en el chat o escribe <red>'cancelar'</red>.</gold>");
                break;
            case CHEST:
                int amount = (event.getClick() == ClickType.LEFT) ? 1 : -1;
                if (event.isShiftClick()) amount *= 10;
                if (event.getClick() == ClickType.MIDDLE) {
                    shopItem.setInfiniteStock(!shopItem.isInfiniteStock());
                } else {
                    shopItem.setStock(Math.max(0, shopItem.getStock() + amount));
                }
                plugin.getShopManager().setItem(slot, shopItem);
                plugin.getShopGUI().openItemEditor(player, slot, shopItem);
                break;
            case BARRIER:
                plugin.getShopGUI().openEditor(player);
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (editingPrice.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            int slot = editingPrice.get(player.getUniqueId());

            if (event.getMessage().equalsIgnoreCase("cancelar")) {
                editingPrice.remove(player.getUniqueId());
                MessageUtils.sendMessage(player, "<red>Edición de precio cancelada.</red>");
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getShopGUI().openEditor(player));
                return;
            }

            try {
                double newPrice = Double.parseDouble(event.getMessage());
                if (newPrice < 0) {
                    MessageUtils.sendMessage(player, "<red>El precio no puede ser negativo.</red>");
                } else {
                    ShopItem shopItem = plugin.getShopManager().getItem(slot);
                    if (shopItem != null) {
                        shopItem.setPrice(newPrice);
                        plugin.getShopManager().setItem(slot, shopItem);
                        MessageUtils.sendMessage(player, "<green>Precio actualizado a <gold>" + newPrice + "</gold>.</green>");
                    }
                    editingPrice.remove(player.getUniqueId());
                }
            } catch (NumberFormatException e) {
                MessageUtils.sendMessage(player, "<red>Por favor, introduce un número válido.</red>");
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                ShopItem updatedItem = plugin.getShopManager().getItem(slot);
                if (updatedItem != null) {
                    plugin.getShopGUI().openItemEditor(player, slot, updatedItem);
                } else {
                    plugin.getShopGUI().openEditor(player);
                }
            });
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        editingPrice.remove(event.getPlayer().getUniqueId());
    }
}