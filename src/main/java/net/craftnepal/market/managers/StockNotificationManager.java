package net.craftnepal.market.managers;

import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.Market;
import net.craftnepal.market.utils.SendMessage;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the periodic background task that reminds online plot owners about their out-of-stock shops.
 * Runs every 15 minutes (18000 ticks) asynchronously to avoid impacting the main thread.
 */
public class StockNotificationManager {

    private static final long CHECK_INTERVAL_TICKS = 18000L; // 15 minutes

    public static void startPeriodicChecks() {
        Bukkit.getScheduler().runTaskTimer(Market.getPlugin(),
                StockNotificationManager::checkAndNotifyOnlinePlayers,
                CHECK_INTERVAL_TICKS,
                CHECK_INTERVAL_TICKS);
    }

    /**
     * Scans all shops for every online player and sends a chat reminder
     * if any of their selling shops are empty.
     */
    public static void checkAndNotifyOnlinePlayers() {
        Map<String, ChestShop> allShops = ShopUtils.getAllShops();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            List<String> outOfStockItems = new ArrayList<>();
            int emptyShops = 0;

            for (ChestShop shop : allShops.values()) {
                if (shop.getOwner() == null || !shop.getOwner().equals(uuid)) continue;
                if (shop.isAdmin() || shop.isBuyingShop()) continue;

                if (ShopUtils.getShopStock(shop) == 0) {
                    emptyShops++;
                    String name = ShopUtils.getShopDisplayName(shop);
                    if (!outOfStockItems.contains(name)) {
                        outOfStockItems.add(name);
                    }
                }
            }

            if (emptyShops > 0) {
                SendMessage.sendPlayerMessage(player,
                        "§c[Reminder] You have " + emptyShops + " shop(s) out of stock! Missing: "
                                + String.join(", ", outOfStockItems));
                SendMessage.sendPlayerMessage(player,
                        "§eUse §6/market plot manage §eto view your out of stock items.");
            }
        }
    }
}
