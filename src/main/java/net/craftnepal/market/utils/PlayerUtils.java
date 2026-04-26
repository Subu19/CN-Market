package net.craftnepal.market.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import net.craftnepal.market.files.LocationData;

import java.util.UUID;

public class PlayerUtils {

    public static ItemStack getPlayerHead(UUID playerUUID) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUUID));
        head.setItemMeta(meta);
        return head;
    }

    public static void saveLastLocation(Player player) {
        saveLastLocation(player, player.getLocation());
    }

    public static void saveLastLocation(Player player, Location location) {
        net.craftnepal.market.utils.LocationUtils.saveLocation(LocationData.get(), "players."+player.getUniqueId().toString(), location);
        LocationData.save();
    }

    public static Location getLastLocation(Player player) {
        return net.craftnepal.market.utils.LocationUtils.loadLocation(LocationData.get(), "players."+player.getUniqueId().toString());
    }

    public static void clearLastLocation(Player player) {
        LocationData.get().set("players."+player.getUniqueId().toString(), null);
        LocationData.save();
    }
}