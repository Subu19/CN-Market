package net.craftnepal.market.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.List;

public class CommandTabFilter implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(TabCompleteEvent event) {
        filterAdmin(event.getSender(), event.getBuffer(), event.getCompletions());
    }

    private void filterAdmin(org.bukkit.command.CommandSender sender, String buffer, List<String> completions) {
        // Check if the command starts with /market
        if (buffer.startsWith("/market") || buffer.startsWith("/market ")) {
            String[] args = buffer.split(" ");
            
            // If it's the first argument level (e.g., "/market ")
            // buffer might be "/market "
            
            // If buffer is "/market ", it means they are looking for subcommands
            if (buffer.endsWith(" ") && args.length == 1 && args[0].equalsIgnoreCase("/market")) {
                if (!sender.hasPermission("market.admin")) {
                    completions.remove("admin");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("/market")) {
                // They are typing the first subcommand, e.g., "/market a"
                if (!sender.hasPermission("market.admin")) {
                    completions.remove("admin");
                }
            }
        }
    }
}
