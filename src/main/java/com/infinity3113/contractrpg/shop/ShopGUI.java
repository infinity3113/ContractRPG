package com.infinity3113.contractrpg.shop;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ShopGUI {

    private final ContractRPG plugin;

    public ShopGUI(ContractRPG plugin) {
        this.plugin = plugin;
    }

    public void openShop(Player player) {
        // Implementa aquí la lógica para abrir la tienda para jugadores normales si es necesario.
        // Por ahora, la dejamos vacía ya que la petición se centra en el editor.
        MessageUtils.sendMessage(player, "<red>La tienda para jugadores aún no está implementada.</red>");
    }

    public void openEditor(Player player) {
        // Obtenemos el título del editor desde el yml y lo procesamos con MessageUtils
        String title = plugin.getShopManager().getConfig().getString("editor-title", "<bold><#27A6F5>Editor de la Tienda</bold>");
        int size = plugin.getShopManager().getConfig().getInt("shop-size", 54);
        Inventory editor = Bukkit.createInventory(player, size, MessageUtils.parse(title));

        // Cargar y mostrar los items guardados
        for (Map.Entry<Integer, ShopItem> entry : plugin.getShopManager().getShopItems().entrySet()) {
            if (entry.getKey() < size) {
                editor.setItem(entry.getKey(), createEditorDisplayItem(entry.getValue()));
            }
        }

        // Item de información
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(MessageUtils.parse("<yellow><bold>CÓMO FUNCIONA</bold></yellow>"));
        infoMeta.lore(Arrays.asList(
                MessageUtils.parse("<gray>Arrastra un item de tu inventario a un"),
                MessageUtils.parse("<gray>slot vacío para <green><bold>añadirlo</bold></gray> a la tienda."),
                MessageUtils.parse(""),
                MessageUtils.parse("<gray>Click <aqua><bold>IZQUIERDO</bold></aqua> sobre un item para <yellow><bold>editarlo</bold></yellow>."),
                MessageUtils.parse("<gray>Click <red><bold>DERECHO</bold></red> sobre un item para <red><bold>eliminarlo</bold></red>.")
        ));
        info.setItemMeta(infoMeta);

        editor.setItem(size - 1, info);
        player.openInventory(editor);
    }

    public void openItemEditor(Player player, int slot, ShopItem shopItem) {
        Inventory itemEditor = Bukkit.createInventory(player, 27, MessageUtils.parse("<dark_gray>Editando Item (Slot " + slot + ")</dark_gray>"));

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(MessageUtils.parse(" "));
        glass.setItemMeta(meta);
        for (int i = 0; i < itemEditor.getSize(); i++) itemEditor.setItem(i, glass);

        itemEditor.setItem(4, shopItem.getItemStack());

        ItemStack priceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta priceMeta = priceItem.getItemMeta();
        priceMeta.displayName(MessageUtils.parse("<yellow><bold>Editar Precio</bold></yellow>"));
        priceMeta.lore(Arrays.asList(
                MessageUtils.parse("<gray>Precio actual: <gold>" + shopItem.getPrice() + "</gold>"),
                MessageUtils.parse("<white>Click para cambiar el precio en el chat.</white>")
        ));
        priceItem.setItemMeta(priceMeta);
        itemEditor.setItem(11, priceItem);

        ItemStack stockItem = new ItemStack(Material.CHEST);
        ItemMeta stockMeta = stockItem.getItemMeta();
        stockMeta.displayName(MessageUtils.parse("<aqua><bold>Editar Stock</bold></aqua>"));
        stockMeta.lore(Arrays.asList(
                MessageUtils.parse("<gray>Stock actual: <blue>" + (shopItem.isInfiniteStock() ? "Infinito" : shopItem.getStock()) + "</blue>"),
                MessageUtils.parse("<white>Click IZQUIERDO para añadir stock.</white>"),
                MessageUtils.parse("<white>Click DERECHO para quitar stock.</white>"),
                MessageUtils.parse("<white>Click MEDIO (rueda) para stock infinito.</white>")
        ));
        stockItem.setItemMeta(stockMeta);
        itemEditor.setItem(13, stockItem);

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(MessageUtils.parse("<red><bold>Volver al Editor Principal</bold></red>"));
        back.setItemMeta(backMeta);
        itemEditor.setItem(22, back);

        player.openInventory(itemEditor);
    }

    private ItemStack createEditorDisplayItem(ShopItem shopItem) {
        ItemStack display = shopItem.getItemStack().clone();
        ItemMeta meta = display.getItemMeta();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(MessageUtils.parse("<gray>--------------------</gray>"));
        lore.add(MessageUtils.parse("<yellow><bold>DATOS DE LA TIENDA</bold></yellow>"));
        lore.add(MessageUtils.parse("<gray>Precio: <gold>" + shopItem.getPrice() + " Puntos</gold>"));
        lore.add(MessageUtils.parse("<gray>Stock: <aqua>" + (shopItem.isInfiniteStock() ? "Infinito" : shopItem.getStock()) + "</aqua>"));
        lore.add(MessageUtils.parse("<gray>--------------------</gray>"));
        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }
}