package net.craftnepal.market.Entities;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ChestShop {
    private String id;
    private Location location;
    private ItemStack item;
    private UUID owner;
    private double price;
    private boolean isAdmin;
    private boolean isBuyingShop; // true if the shop BUYS items from players (Player sells to shop)

    public ChestShop(String id, Location location, ItemStack item, UUID owner, double price) {
        this(id, location, item, owner, price, false, false);
    }

    public ChestShop(String id, Location location, ItemStack item, UUID owner, double price, boolean isAdmin, boolean isBuyingShop) {
        this.id = id;
        this.location = location;
        this.item = item;
        this.owner = owner;
        this.price = price;
        this.isAdmin = isAdmin;
        this.isBuyingShop = isBuyingShop;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean isBuyingShop() {
        return isBuyingShop;
    }

    public void setBuyingShop(boolean buyingShop) {
        isBuyingShop = buyingShop;
    }
}