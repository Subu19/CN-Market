package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetPlotSpawn extends SubCommand {
    @Override
    public String getName() {
        return "setplotspawn";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Sets spawn point for your plot";
    }

    @Override
    public String getSyntax() {
        return "/amarket setplotspawn";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("This command can only be used by players!");
            return;
        }

        Player player = (Player) commandSender;
        Location location = player.getLocation();

        // Check if player is in their own plot
        if (!PlotUtils.isPlayerInOwnPlot(player)) {
            SendMessage.sendPlayerMessage(player, "&cYou must be in your own plot to set its spawn point!");
            return;
        }

        String plotId = PlotUtils.getPlotIdByLocation(location);
        PlotUtils.setPlotSpawn(plotId, location);
        SendMessage.sendPlayerMessage(player, "&aPlot spawn point set successfully!");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
