package com.infinity3113.contractrpg.shop;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ItemSerializer {

    public static Map<String, Object> serialize(ShopItem shopItem) {
        Map<String, Object> map = new HashMap<>();
        map.put("item", shopItem.getItemStack());
        map.put("price", shopItem.getPrice());
        map.put("stock", shopItem.getStock());
        map.put("infinite-stock", shopItem.isInfiniteStock());
        map.put("cooldown", shopItem.getCooldown());
        // La sección de comandos ha sido eliminada.
        return map;
    }

    public static ShopItem deserialize(ConfigurationSection section) {
        ItemStack item = section.getItemStack("item");
        double price = section.getDouble("price", 0);
        int stock = section.getInt("stock", 1);
        boolean infiniteStock = section.getBoolean("infinite-stock", false);
        long cooldown = section.getLong("cooldown", 0);
        // La deserialización de comandos ha sido eliminada.
        return new ShopItem(item, price, stock, infiniteStock, cooldown);
    }
}