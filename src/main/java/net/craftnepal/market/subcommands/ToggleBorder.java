package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;
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

            if(Objects.equals(strings[1], "market")){

                Location min = RegionData.get().getLocation("market.posMin");
                Location max = RegionData.get().getLocation("market.posMax");
                if(max !=null && min !=null){
                    RegionUtils.visibleRegionBorders(player,min,max, Market.getPlugin(), Color.RED);
                }else{
                    player.sendMessage("Couldn't find market regions! Make sure you have created it.");
                }

            }else if(Objects.equals(strings[1], "plot" )){
                if(strings.length == 3){

                    Location min = RegionData.get().getLocation("market.plots."+Integer.parseInt(strings[2])+".posMin");
                    Location max = RegionData.get().getLocation("market.plots."+Integer.parseInt(strings[2])+".posMax");
                    if(max !=null && min !=null){
                        RegionUtils.visibleRegionBorders(player,min,max,Market.getPlugin(), Color.LIME,100);
                    }else{
                        player.sendMessage("Couldn't find the plot.");
                    }

                }else{

                    player.sendMessage("Please put plot number. Usage /amarket toggleborder plot <plot.No>");

                }


            }else{

                player.sendMessage("No region in argument!");

            }

        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        List<String> autoComplete =  new ArrayList<>();
        autoComplete.add("market");
        autoComplete.add("plot");
        return autoComplete;
    }
}
