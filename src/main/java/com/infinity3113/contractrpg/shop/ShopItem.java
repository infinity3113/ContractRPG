package com.infinity3113.contractrpg.shop;

import org.bukkit.inventory.ItemStack;

// NOTA: Se ha eliminado la lista de comandos de esta clase.
public class ShopItem {

    private final ItemStack itemStack;
    private double price;
    private int stock;
    private boolean infiniteStock;
    private long cooldown;

    public ShopItem(ItemStack itemStack, double price, int stock, boolean infiniteStock, long cooldown) {
        this.itemStack = itemStack;
        this.price = price;
        this.stock = stock;
        this.infiniteStock = infiniteStock;
        this.cooldown = cooldown;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public boolean isInfiniteStock() {
        return infiniteStock;
    }

    public void setInfiniteStock(boolean infiniteStock) {
        this.infiniteStock = infiniteStock;
    }

    public long getCooldown() {
        return cooldown;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }
}