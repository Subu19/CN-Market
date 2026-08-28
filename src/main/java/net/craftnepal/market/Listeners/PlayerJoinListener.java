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
        Bukkit.getScheduler().runTaskAsynchronously(Market.getPlugin(), () -> {
            List<net.craftnepal.market.managers.DatabaseManager.OfflineSale> sales = net.craftnepal.market.managers.DatabaseManager.getOfflineSales(uuid.toString());
            double legacyOfflineEarnings = net.craftnepal.market.managers.DatabaseManager.getOfflineEarnings(uuid.toString());
            
            if (!sales.isEmpty()) {
                double totalEarnings = 0;
                int totalQuantity = 0;
                for (net.craftnepal.market.managers.DatabaseManager.OfflineSale sale : sales) {
                    totalEarnings += sale.getEarnings();
                    totalQuantity += sale.getQuantity();
                }

                final double finalTotalEarnings = totalEarnings;
                final int finalTotalQuantity = totalQuantity;

                Bukkit.getScheduler().runTaskLater(Market.getPlugin(), () -> {
                    if (player.isOnline()) {
                        String prefix = me.kodysimpson.simpapi.colors.ColorTranslator.translateColorCodes(Market.getMainConfig().getString("prefix"));
                        
                        StringBuilder hoverText = new StringBuilder();
                        for (int i = 0; i < sales.size(); i++) {
                            net.craftnepal.market.managers.DatabaseManager.OfflineSale sale = sales.get(i);
                            hoverText.append("§7").append(sale.getItemDisplayName()).append(" §8(§7x").append(sale.getQuantity()).append("§8)");
                            if (i < sales.size() - 1) {
                                hoverText.append("\n");
                            }
                        }
                        hoverText.append("\n§7Profit: §a").append(EconomyUtils.format(finalTotalEarnings));

                        net.md_5.bungee.api.chat.TextComponent mainComponent = new net.md_5.bungee.api.chat.TextComponent(
                                prefix + "§fSold §e" + finalTotalQuantity + " §fitems for §a" + EconomyUtils.format(finalTotalEarnings) + "§f. "
                        );
                        net.md_5.bungee.api.chat.TextComponent hoverComponent = new net.md_5.bungee.api.chat.TextComponent("§7[Hover for details]");
                        hoverComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                                new net.md_5.bungee.api.chat.ComponentBuilder(hoverText.toString()).create()
                        ));
                        mainComponent.addExtra(hoverComponent);

                        player.spigot().sendMessage(mainComponent);

                        // Clear offline earnings and sales asynchronously
                        Bukkit.getScheduler().runTaskAsynchronously(Market.getPlugin(), () -> {
                            net.craftnepal.market.managers.DatabaseManager.setOfflineEarnings(uuid.toString(), 0.0);
                            net.craftnepal.market.managers.DatabaseManager.clearOfflineSales(uuid.toString());
                        });
                    }
                }, 60L); // 3 seconds after join
            } else if (legacyOfflineEarnings > 0) {
                Bukkit.getScheduler().runTaskLater(Market.getPlugin(), () -> {
                    if (player.isOnline()) {
                        SendMessage.sendPlayerMessage(player, "§aWhile you were offline, your market shops earned you " + EconomyUtils.format(legacyOfflineEarnings) + "!");
                        // Clear offline earnings asynchronously
                        Bukkit.getScheduler().runTaskAsynchronously(Market.getPlugin(), () -> {
                            net.craftnepal.market.managers.DatabaseManager.setOfflineEarnings(uuid.toString(), 0.0);
                        });
                    }
                }, 60L); // 3 seconds after join
            }
        });

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
