package net.craftnepal.market.Listeners;

import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.Market;
import net.craftnepal.market.utils.EconomyUtils;
import net.craftnepal.market.utils.SendMessage;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Notify offline earnings
        double offlineEarnings = net.craftnepal.market.managers.DatabaseManager.getOfflineEarnings(uuid.toString());
        if (offlineEarnings > 0) {
            Bukkit.getScheduler().runTaskLater(Market.getPlugin(), () -> {
                SendMessage.sendPlayerMessage(player, "§aWhile you were offline, your market shops earned you " + EconomyUtils.format(offlineEarnings) + "!");
                // Clear offline earnings
                net.craftnepal.market.managers.DatabaseManager.setOfflineEarnings(uuid.toString(), 0.0);
            }, 60L); // 3 seconds after join
        }

        // Notify low stock
        Bukkit.getScheduler().runTaskLater(Market.getPlugin(), () -> {
            List<ChestShop> playerShops = ShopUtils.getPlayerSellingShops(uuid);
            List<String> outOfStockItems = new ArrayList<>();
            int emptyShops = 0;
            
            for (ChestShop shop : playerShops) {
                int stock = ShopUtils.getShopStock(shop);
                if (stock == 0) {
                    emptyShops++;
                    String name = ShopUtils.getShopDisplayName(shop);
                    if (!outOfStockItems.contains(name)) {
                        outOfStockItems.add(name);
                    }
                }
            }
            
            if (emptyShops > 0) {
                SendMessage.sendPlayerMessage(player, "§cYou have " + emptyShops + " shop(s) out of stock! Items missing: " + String.join(", ", outOfStockItems));
                SendMessage.sendPlayerMessage(player, "§eUse §6/market plot manage §eto check your out of stock items.");
            }
        }, 100L);

    }
}
