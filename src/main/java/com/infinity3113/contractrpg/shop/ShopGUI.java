package com.infinity3113.contractrpg.shop;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ShopGUI {

    private final ContractRPG plugin;
    private final NamespacedKey shopItemIdKey;

    public ShopGUI(ContractRPG plugin) {
        this.plugin = plugin;
        this.shopItemIdKey = new NamespacedKey(plugin, "shop-item-id");
    }

    public void openShop(Player player) {
        String title = MessageUtils.parse(plugin.getShopManager().getConfig().getString("shop-title", "<bold>Tienda</bold>"));
        int size = plugin.getShopManager().getConfig().getInt("shop-size", 54);
        Inventory gui = Bukkit.createInventory(null, size, title);
        PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());

        ConfigurationSection itemsSection = plugin.getShopManager().getItemsSection();
        if (itemsSection == null) {
            player.sendMessage(MessageUtils.parse("<red>La tienda está vacía."));
            return;
        }

        for (String id : itemsSection.getKeys(false)) {
            ItemStack item = ItemSerializer.deserialize(itemsSection.getString(id + ".item"));
            if (item == null) continue;

            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(" ");

            int price = itemsSection.getInt(id + ".price");
            long stockResetSeconds = itemsSection.getLong(id + ".stock-reset-seconds");
            boolean purchased = playerData.getPurchasedShopItems().containsKey(id);
            boolean onCooldown = false;

            if (purchased) {
                long purchaseTime = playerData.getPurchasedShopItems().get(id);
                long cooldownMillis = TimeUnit.SECONDS.toMillis(stockResetSeconds);
                if (System.currentTimeMillis() - purchaseTime < cooldownMillis) {
                    onCooldown = true;
                }
            }

            if (onCooldown) {
                long remainingMillis = (playerData.getPurchasedShopItems().get(id) + TimeUnit.SECONDS.toMillis(stockResetSeconds)) - System.currentTimeMillis();
                lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("shop.item.cooldown")
                    .replace("%time%", formatTime(remainingMillis))));
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); 
            } else {
                if (playerData.getContractPoints() >= price) {
                    lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("shop.item.buyable")
                        .replace("%price%", String.valueOf(price))));
                } else {
                    lore.add(MessageUtils.parse(plugin.getLangManager().getMessage("shop.item.no-funds")
                        .replace("%price%", String.valueOf(price))));
                }
            }

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(shopItemIdKey, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
            gui.setItem(itemsSection.getInt(id + ".slot"), item);
        }
        player.openInventory(gui);
    }
    
    public void openEditor(Player player) {
        String title = MessageUtils.parse("<bold>Editor de Tienda</bold>");
        int size = plugin.getShopManager().getConfig().getInt("shop-size", 54);
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        ConfigurationSection itemsSection = plugin.getShopManager().getItemsSection();
        if (itemsSection != null) {
            for (String id : itemsSection.getKeys(false)) {
                ItemStack item = ItemSerializer.deserialize(itemsSection.getString(id + ".item"));
                if (item == null) continue;
                
                ItemMeta meta = item.getItemMeta();
                List<String> lore = new ArrayList<>();
                lore.add(" ");
                lore.add(MessageUtils.parse("<yellow>ID: <white>" + id));
                lore.add(MessageUtils.parse("<yellow>Precio: <white>" + itemsSection.getInt(id + ".price")));
                lore.add(MessageUtils.parse("<yellow>Cooldown: <white>" + itemsSection.getInt(id + ".stock-reset-seconds") + "s"));
                lore.add(" ");
                lore.add(MessageUtils.parse("<red><bold>SHIFT+CLICK IZQ</bold> para ELIMINAR"));
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(shopItemIdKey, PersistentDataType.STRING, id);
                item.setItemMeta(meta);
                
                gui.setItem(itemsSection.getInt(id + ".slot"), item);
            }
        }
        
        player.openInventory(gui);
    }

    private String formatTime(long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }
}