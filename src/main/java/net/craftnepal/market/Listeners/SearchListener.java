package net.craftnepal.market.Listeners;

import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.MenuManager;
import net.craftnepal.market.Market;
import net.craftnepal.market.menus.AllItemsMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SearchListener implements Listener {

    private static final Set<UUID> searchers = new HashSet<>();

    public static void addSearcher(Player player) {
        searchers.add(player.getUniqueId());
        player.sendMessage(ChatColor.YELLOW + "=============================");
        player.sendMessage(ChatColor.GREEN + "Type your search query in chat.");
        player.sendMessage(ChatColor.GRAY + "Type " + ChatColor.RED + "cancel" + ChatColor.GRAY + " to exit search.");
        player.sendMessage(ChatColor.YELLOW + "=============================");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (searchers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage().trim();

            if (message.equalsIgnoreCase("cancel")) {
                searchers.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "Search cancelled.");
                return;
            }

            searchers.remove(player.getUniqueId());

            // Open menu synchronously
            Bukkit.getScheduler().runTask(Market.getPlugin(), () -> {
                try {
                    AllItemsMenu menu = new AllItemsMenu(MenuManager.getPlayerMenuUtility(player));
                    menu.setSearchQuery(message);
                    menu.open();
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "An error occurred while opening the search menu.");
                    e.printStackTrace();
                }
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        searchers.remove(event.getPlayer().getUniqueId());
    }
}
