package com.infinity3113.contractrpg.shop;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemSerializer {

    public static Map<String, Object> serialize(ShopItem shopItem) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemStack", shopItem.getItemStack().serialize());
        map.put("price", shopItem.getPrice());
        map.put("stock", shopItem.getStock());
        map.put("infiniteStock", shopItem.isInfiniteStock());
        map.put("cooldown", shopItem.getCooldown()); // <-- AÑADIDO
        map.put("commands", shopItem.getCommands()); // <-- AÑADIDO
        return map;
    }

    @SuppressWarnings("unchecked")
    public static ShopItem deserialize(ConfigurationSection section) {
        Map<String, Object> itemStackMap = section.getConfigurationSection("itemStack").getValues(true);
        ItemStack itemStack = ItemStack.deserialize(itemStackMap);

        double price = section.getDouble("price", 0);
        int stock = section.getInt("stock", 0);
        boolean infiniteStock = section.getBoolean("infiniteStock", false);
        long cooldown = section.getLong("cooldown", 0); // <-- AÑADIDO
        List<String> commands = section.getStringList("commands"); // <-- AÑADIDO

        return new ShopItem(itemStack, price, stock, infiniteStock, cooldown, commands);
    }
}