package net.craftnepal.market.subcommands.plot;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.utils.PlotUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PlotSpawn extends SubCommand {
    @Override
    public String getName() {
        return "spawn";
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
        return "/market plot spawn";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("This command can only be used by players!");
            return;
        }

        Player player = (Player) commandSender;

        if (!player.hasPermission("market.plot.teleport")) {
            net.craftnepal.market.utils.SendMessage.sendPlayerMessage(player, "&cYou do not have permission to teleport to plots.");
            return;
        }

        PlotUtils.teleportToOwnPlotSpawn(player);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
