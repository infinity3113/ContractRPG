package com.infinity3113.contractrpg.shop;

import org.bukkit.inventory.ItemStack;

public class ShopItem {

    private ItemStack itemStack;
    private double price;
    private int stock;
    private boolean infiniteStock;

    public ShopItem(ItemStack itemStack, double price, int stock, boolean infiniteStock) {
        this.itemStack = itemStack;
        this.price = price;
        this.stock = stock;
        this.infiniteStock = infiniteStock;
    }

    // Getters
    public ItemStack getItemStack() { return itemStack; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public boolean isInfiniteStock() { return infiniteStock; }

    // Setters
    public void setItemStack(ItemStack itemStack) { this.itemStack = itemStack; }
    public void setPrice(double price) { this.price = price; }
    public void setStock(int stock) { this.stock = stock; }
    public void setInfiniteStock(boolean infiniteStock) { this.infiniteStock = infiniteStock; }
}