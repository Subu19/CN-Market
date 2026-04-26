package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.RegionUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ToggleBorder extends SubCommand {
    @Override
    public String getName() {
        return "toggleborder";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Make border of plots/market visible or invisible.";
    }

    @Override
    public String getSyntax() {
        return "/amarket toggleborder <region> [plot number]";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;

            if(Objects.equals(strings[1], "plot" )){
                String plotId = null;
                if(strings.length == 3){
                    plotId = strings[2];
                } else {
                    plotId = PlotUtils.getPlotIdByLocation(player.getLocation());
                }

                if (plotId != null) {
                    Location min = RegionData.get().getLocation("market.plots." + plotId + ".posMin");
                    Location max = RegionData.get().getLocation("market.plots." + plotId + ".posMax");
                    if(max !=null && min !=null){
                        RegionUtils.visibleRegionBorders(player,min,max,Market.getPlugin(), Color.LIME,100);
                        player.sendMessage("Toggled border for plot: " + plotId);
                    }else{
                        player.sendMessage("Couldn't find data for plot: " + plotId);
                    }
                } else {
                    player.sendMessage("No plot found at your location. Please specify a plot ID.");
                }
            }else{
                player.sendMessage("Invalid usage. /amarket toggleborder plot [plotId]");
            }

        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        List<String> autoComplete =  new ArrayList<>();
        autoComplete.add("plot");
        return autoComplete;
    }
}
