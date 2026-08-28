package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.managers.DatabaseManager;
import net.craftnepal.market.utils.DisplayUtils;
import net.craftnepal.market.utils.SendMessage;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class SyncStock extends SubCommand {

    @Override
    public String getName() {
        return "syncstock";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Forcefully synchronize all player shop stock caches with physical barrels.";
    }

    @Override
    public String getSyntax() {
        return "/market admin syncstock";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("market.admin")) {
            SendMessage.sendPlayerMessage((Player) sender, "§cYou don't have permission to use this command.");
            return;
        }

        String startMsg = "§aStarting force-synchronization of all player shop stocks...";
        if (sender instanceof Player) {
            SendMessage.sendPlayerMessage((Player) sender, startMsg);
        } else {
            Bukkit.getLogger().info(startMsg);
        }

        Map<String, ChestShop> allShops = ShopUtils.getAllShops();
        int count = 0;

        for (ChestShop shop : allShops.values()) {
            if (shop.isAdmin()) continue;

            Location loc = shop.getLocation();
            if (loc == null) continue;

            Chunk chunk = loc.getChunk();
            boolean wasLoaded = chunk.isLoaded();
            if (!wasLoaded) {
                chunk.load();
            }

            try {
                int physicalStock = ShopUtils.getPhysicalBarrelStock(shop);
                DatabaseManager.updateShopStock(shop.getId(), physicalStock);
                
                // Trigger display update so hologram/display is refreshed
                DisplayUtils.getInstance().updateDisplay(shop);
                count++;
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Market] Failed to sync stock for shop " + shop.getId() + " at " + loc.toString());
                e.printStackTrace();
            } finally {
                if (!wasLoaded) {
                    chunk.unload();
                }
            }
        }

        String completeMsg = "§aSuccessfully synchronized stock for " + count + " player shops!";
        if (sender instanceof Player) {
            SendMessage.sendPlayerMessage((Player) sender, completeMsg);
        } else {
            Bukkit.getLogger().info(completeMsg);
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
