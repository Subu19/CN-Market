package net.craftnepal.market.utils;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class EconomyUtils {

    private static Economy econ = null;

    public static boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public static boolean hasBalance(UUID playerUUID, double amount) {
        if (econ == null) return false;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return econ.has(player, amount);
    }

    public static boolean withdraw(UUID playerUUID, double amount) {
        if (econ == null) return false;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }

    public static boolean deposit(UUID playerUUID, double amount) {
        if (econ == null) return false;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return econ.depositPlayer(player, amount).transactionSuccess();
    }

    public static double getBalance(UUID playerUUID) {
        if (econ == null) return 0;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return econ.getBalance(player);
    }

    public static String format(double amount) {
        if (econ == null) return "$" + amount;
        return econ.format(amount);
    }
}
