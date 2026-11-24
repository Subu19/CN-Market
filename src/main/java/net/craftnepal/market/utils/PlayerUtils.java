package net.craftnepal.market.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import net.craftnepal.market.managers.DatabaseManager;

import java.util.UUID;

public class PlayerUtils {

    private static final java.util.Map<UUID, String> nameCache = new java.util.HashMap<>();

    /**
     * Last known name of a player, cached so repeated lookups do not hit the player data files.
     * Main-thread only.
     */
    public static String getName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            nameCache.put(uuid, online.getName());
            return online.getName();
        }
        return nameCache.computeIfAbsent(uuid, id -> {
            String name = Bukkit.getOfflinePlayer(id).getName();
            return name != null ? name : "Unknown";
        });
    }

    /**
     * A player who has joined this server, by name: online players first, then local player data.
     * Unlike {@code Bukkit.getOfflinePlayer(String)} this never asks Mojang, so it cannot block
     * the server thread on a web request. Null when no such player has joined.
     */
    public static OfflinePlayer findKnownPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        // ponytail: scans every known player; fine for a command, add a name index if it is ever hot.
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (name.equalsIgnoreCase(p.getName())) return p;
        }
        return null;
    }

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
        DatabaseManager.saveLastLocation(player.getUniqueId().toString(), location);
    }

    public static Location getLastLocation(Player player) {
        return DatabaseManager.getLastLocation(player.getUniqueId().toString());
    }

    public static void clearLastLocation(Player player) {
        DatabaseManager.clearLastLocation(player.getUniqueId().toString());
    }
}