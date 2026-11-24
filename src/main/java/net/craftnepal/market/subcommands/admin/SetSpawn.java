package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.files.RegionData;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetSpawn extends SubCommand {
    @Override
    public String getName() {
        return "setspawn";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Sets spawn of market.";
    }

    @Override
    public String getSyntax() {
        return "/market admin setspawn";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;
            Location location = p.getLocation();
            // Saving elsewhere would also overwrite that other world's spawn point.
            if (!net.craftnepal.market.utils.MarketUtils.isInMarketWorld(location.getWorld())) {
                net.craftnepal.market.utils.SendMessage.sendPlayerMessage(p, "&cStand in the market world to set its spawn.");
                return;
            }
            
            // Save to SQLite database
            net.craftnepal.market.managers.DatabaseManager.setMarketSpawn(location);
            
            // Update actual world spawn immediately
            if (location.getWorld() != null) {
                location.getWorld().setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            }
            
            net.craftnepal.market.utils.SendMessage.sendPlayerMessage(p, "&aMarket spawn location has been set to your current position!");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
