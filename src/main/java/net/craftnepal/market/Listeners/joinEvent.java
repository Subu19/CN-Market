package net.craftnepal.market.Listeners;

import net.craftnepal.market.Market;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class joinEvent implements Listener {
    FileConfiguration config;
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player p = event.getPlayer();
        String joinmessage = config.getString("join-message");
        if(joinmessage != null){
            event.setJoinMessage(ChatColor.translateAlternateColorCodes('&',joinmessage.replace("%player%", event.getPlayer().getDisplayName())));
        }
    }

    public joinEvent(Plugin plugin) {
        this.config = Market.getMainConfig();
    }
}
