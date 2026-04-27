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

    public ChestShop(String id, Location location, ItemStack item, UUID owner, double price) {
        this.id = id;
        this.location = location;
        this.item = item;
        this.owner = owner;
        this.price = price;
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
}