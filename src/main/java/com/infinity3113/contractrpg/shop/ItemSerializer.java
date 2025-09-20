package com.infinity3113.contractrpg.shop;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ItemSerializer {

    public static Map<String, Object> serialize(ShopItem shopItem) {
        Map<String, Object> map = new HashMap<>();
        // Serializa el ItemStack completo, conservando todos sus datos (NBT, etc.)
        map.put("itemStack", shopItem.getItemStack().serialize());
        map.put("price", shopItem.getPrice());
        map.put("stock", shopItem.getStock());
        map.put("infiniteStock", shopItem.isInfiniteStock());
        return map;
    }

    @SuppressWarnings("unchecked")
    public static ShopItem deserialize(ConfigurationSection section) {
        // Deserializa el ItemStack
        Map<String, Object> itemStackMap = section.getConfigurationSection("itemStack").getValues(true);
        ItemStack itemStack = ItemStack.deserialize(itemStackMap);

        // Obtiene los demás datos
        double price = section.getDouble("price", 0);
        int stock = section.getInt("stock", 0);
        boolean infiniteStock = section.getBoolean("infiniteStock", false);

        return new ShopItem(itemStack, price, stock, infiniteStock);
    }
}