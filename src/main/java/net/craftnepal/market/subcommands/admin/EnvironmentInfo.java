package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Market;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** Shows the market world environment settings in effect. */
public class EnvironmentInfo extends SubCommand {

    @Override
    public String getName() { return "environment"; }

    @Override
    public List<String> getAliases() { return null; }

    @Override
    public String getDescription() { return "Show the market world environment settings"; }

    @Override
    public String getSyntax() { return "/market admin environment"; }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!sender.hasPermission("market.admin")) {
            SendMessage.sendPlayerMessage(sender, "§cYou don't have permission.");
            return;
        }
        SendMessage.sendPlayerMessage(sender, "§7Market world environment (config: market-world.environment):");
        for (String line : Market.settings().environment().describe()) {
            SendMessage.sendPlayerMessage(sender, "§e" + line);
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) { return null; }
}
