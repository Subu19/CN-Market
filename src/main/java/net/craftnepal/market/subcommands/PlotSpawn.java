package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.utils.PlotUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PlotSpawn extends SubCommand {
    @Override
    public String getName() {
        return "plotspawn";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Teleport to your plot's spawn point";
    }

    @Override
    public String getSyntax() {
        return "/market plotspawn";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("This command can only be used by players!");
            return;
        }

        Player player = (Player) commandSender;
        PlotUtils.teleportToOwnPlotSpawn(player);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
