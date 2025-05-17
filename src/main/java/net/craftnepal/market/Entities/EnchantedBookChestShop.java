package net.craftnepal.market.Entities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.Map;
import java.util.UUID;

public class EnchantedBookChestShop extends ChestShop {
    private Map.Entry<Enchantment, Integer> enchantment;

    public EnchantedBookChestShop(String id, Location location, Material item, UUID owner, double price,
            Map.Entry<Enchantment, Integer> enchantment) {
        super(id, location, item, owner, price);
        this.enchantment = enchantment;
    }

    public Map.Entry<Enchantment, Integer> getEnchantment() {
        return enchantment;
    }

    public void setEnchantment(Map.Entry<Enchantment, Integer> enchantment) {
        this.enchantment = enchantment;
    }
}
