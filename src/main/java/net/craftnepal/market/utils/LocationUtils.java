package net.craftnepal.market.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class LocationUtils {

    public static void saveLocation(ConfigurationSection section, String path, Location location) {
        if (location == null || location.getWorld() == null) return;
        
        section.set(path + ".world", location.getWorld().getName());
        section.set(path + ".x", location.getX());
        section.set(path + ".y", location.getY());
        section.set(path + ".z", location.getZ());
        section.set(path + ".yaw", (double) location.getYaw());
        section.set(path + ".pitch", (double) location.getPitch());
    }

    public static Location loadLocation(ConfigurationSection section, String path) {
        if (!section.contains(path + ".world")) return null;
        
        String worldName = section.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        
        double x = section.getDouble(path + ".x");
        double y = section.getDouble(path + ".y");
        double z = section.getDouble(path + ".z");
        float yaw = (float) section.getDouble(path + ".yaw", 0);
        float pitch = (float) section.getDouble(path + ".pitch", 0);
        
        return new Location(world, x, y, z, yaw, pitch);
    }
}
