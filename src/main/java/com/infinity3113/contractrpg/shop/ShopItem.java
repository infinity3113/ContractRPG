package com.infinity3113.contractrpg.shop;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.ArrayList;

public class ShopItem {

    private ItemStack itemStack;
    private double price;
    private int stock;
    private boolean infiniteStock;
    private long cooldown; // <-- AÑADIDO (en segundos)
    private List<String> commands; // <-- AÑADIDO

    public ShopItem(ItemStack itemStack, double price, int stock, boolean infiniteStock, long cooldown, List<String> commands) {
        this.itemStack = itemStack;
        this.price = price;
        this.stock = stock;
        this.infiniteStock = infiniteStock;
        this.cooldown = cooldown; // <-- AÑADIDO
        this.commands = commands != null ? commands : new ArrayList<>(); // <-- AÑADIDO
    }

    // Getters
    public ItemStack getItemStack() { return itemStack; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public boolean isInfiniteStock() { return infiniteStock; }
    public long getCooldown() { return cooldown; } // <-- AÑADIDO
    public List<String> getCommands() { return commands; } // <-- AÑADIDO

    // Setters
    public void setItemStack(ItemStack itemStack) { this.itemStack = itemStack; }
    public void setPrice(double price) { this.price = price; }
    public void setStock(int stock) { this.stock = stock; }
    public void setInfiniteStock(boolean infiniteStock) { this.infiniteStock = infiniteStock; }
    public void setCooldown(long cooldown) { this.cooldown = cooldown; } // <-- AÑADIDO
    public void setCommands(List<String> commands) { this.commands = commands; } // <-- AÑADIDO
}