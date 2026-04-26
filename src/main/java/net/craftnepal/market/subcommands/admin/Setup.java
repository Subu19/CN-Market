package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Market;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class Setup extends SubCommand {

    @Override
    public String getName() {
        return "setup";
    }

    @Override
    public String getDescription() {
        return "Initialize the market world.";
    }

    @Override
    public String getSyntax() {
        return "/amarket setup <worldName> <plotSize> <pathwayWidth>";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        if (!player.hasPermission("market.admin")) {
            SendMessage.sendPlayerMessage(player, "§cYou don't have permission to use this command.");
            return;
        }

        if (args.length < 4) {
            SendMessage.sendPlayerMessage(player, "§cUsage: /amarket setup <worldName> <plotSize> <pathwayWidth>");
            return;
        }

        String worldName = args[1];
        int plotSize;
        int pathwayWidth;

        try {
            plotSize = Integer.parseInt(args[2]);
            pathwayWidth = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            SendMessage.sendPlayerMessage(player, "§cSize and width must be valid numbers.");
            return;
        }

        Market.getMainConfig().set("market-world.name", worldName);
        Market.getMainConfig().set("market-world.plot-size", plotSize);
        Market.getMainConfig().set("market-world.pathway-width", pathwayWidth);
        Market.getMainConfig().set("market-world.is-setup", true);
        Market.getPlugin().saveConfig();

        SendMessage.sendPlayerMessage(player, "§aGenerating market world '" + worldName + "'... This may cause lag.");
        Market.getPlugin().initializeMarketWorld();
        SendMessage.sendPlayerMessage(player, "§aMarket world initialized successfully!");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return null;
    }
}
