package net.craftnepal.market.subcommands.plot;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.RegionUtils;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class Claim extends SubCommand {
    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Claim market plot that you are standing on!";
    }

    @Override
    public String getSyntax() {
        return "/market claim";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;
            Location location = player.getLocation();
            
            // Get the plot ID at the player's location (checks both manual and automatic plots)
            String selectedPlot = PlotUtils.getPlotIdByLocation(location);

            if (selectedPlot != null) {
                // If it's an automatic plot, register it if not already registered
                if (selectedPlot.startsWith("plot_")) {
                    PlotUtils.registerAutomaticPlot(selectedPlot);
                }

                String owner = PlotUtils.getPlotOwner(selectedPlot);
                if (owner == null || owner.isEmpty()) {
                    PlotUtils.setPlotOwner(selectedPlot, player.getUniqueId().toString());
                    SendMessage.sendPlayerMessage(player, "&aYou successfully claimed plot: " + selectedPlot);
                    player.playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                } else if (owner.equals(player.getUniqueId().toString())) {
                    SendMessage.sendPlayerMessage(player, "&aYou already own this plot: " + selectedPlot);
                } else {
                    SendMessage.sendPlayerMessage(player, "&cThis plot is already claimed by someone else.");
                }
            } else {
                SendMessage.sendPlayerMessage(player, "&cNo plot found at your location. (Are you standing on a pathway?)");
                player.playSound(location, Sound.ITEM_SHIELD_BREAK, 1, 1);
            }
        } else {
            Bukkit.getLogger().info("You are not a player");
        }
    }


    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
