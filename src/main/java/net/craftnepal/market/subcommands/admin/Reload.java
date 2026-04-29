package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class Reload extends SubCommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Reload market plugin.";
    }

    @Override
    public String getSyntax() {
        return "/amarket reload";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof Player && !commandSender.hasPermission("market.admin")) {
            SendMessage.sendPlayerMessage((Player) commandSender,
                    "§cYou don't have permission to use this command.");
            return;
        }
        Market.reloadMainConfig();
        RegionData.reload();
        net.craftnepal.market.files.LocationData.reload();
        net.craftnepal.market.files.PriceData.setup(); // Re-reads price.yml
        
        // Re-initialize schematic manager with new config values
        net.craftnepal.market.world.SchematicManager.getInstance().init();
        if (commandSender instanceof Player) {
            SendMessage.sendPlayerMessage((Player) commandSender,
                    "§aMarket configuration reloaded!");
        } else {
            Bukkit.getLogger().info("Market configuration reloaded!");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
