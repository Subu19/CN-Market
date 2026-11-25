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
            SendMessage.sendPlayerMessage(sender, "§cYou don't have permission to use this command.");
            return;
        }

        String startMsg = "§aStarting force-synchronization of all player shop stocks...";
        if (sender instanceof Player) {
            SendMessage.sendPlayerMessage(sender, startMsg);
        } else {
            Bukkit.getLogger().info(startMsg);
        }

        // Barrels are read a few milliseconds' worth per tick: on a big market, reading every barrel
        // (and loading its chunk) in one tick froze the server for over a second.
        java.util.Iterator<ChestShop> shops = new java.util.ArrayList<>(ShopUtils.getAllShops().values()).iterator();
        int[] count = {0};
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                long deadline = System.nanoTime() + BUDGET_NANOS;
                while (shops.hasNext() && System.nanoTime() < deadline) {
                    if (sync(shops.next())) count[0]++;
                }
                if (shops.hasNext()) return;
                cancel();
                String completeMsg = "§aSuccessfully synchronized stock for " + count[0] + " player shops!";
                if (sender instanceof Player p && p.isOnline()) {
                    SendMessage.sendPlayerMessage(p, completeMsg);
                } else {
                    Bukkit.getLogger().info(completeMsg);
                }
            }
        }.runTaskTimer(net.craftnepal.market.Market.getPlugin(), 1L, 1L);
    }

    /** Time spent per tick; one cold chunk load alone can take several milliseconds. */
    private static final long BUDGET_NANOS = 5_000_000L;

    private static boolean sync(ChestShop shop) {
        if (shop.isAdmin() || DatabaseManager.getShop(shop.getId()) == null) return false; // removed meanwhile
        Location loc = shop.getLocation();
        if (loc == null || loc.getWorld() == null) return false;
        Chunk chunk = loc.getChunk();
        boolean wasLoaded = chunk.isLoaded();
        if (!wasLoaded) chunk.load();
        try {
            int physicalStock = ShopUtils.getPhysicalBarrelStock(shop);
            DatabaseManager.updateShopStock(shop.getId(), physicalStock);
            DisplayUtils.getInstance().updateDisplay(shop);
            return true;
        } catch (Exception e) {
            net.craftnepal.market.Market.getPlugin().getLogger().log(java.util.logging.Level.WARNING,
                    "Failed to sync stock for shop " + shop.getId() + " at " + loc, e);
            return false;
        } finally {
            if (!wasLoaded) chunk.unload();
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
