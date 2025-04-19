package net.craftnepal.market.commands;

import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.RegionUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public class MarketVisibleBorder implements CommandExecutor {
    Plugin plugin;
    public MarketVisibleBorder(Plugin p){
        this.plugin = p;
    }
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;
            if(Objects.equals(strings[0], "market")){
                Location min = RegionData.get().getLocation("market.posMin");
                Location max = RegionData.get().getLocation("market.posMax");
                if(max !=null && min !=null){
                    RegionUtils.visibleRegionBorders(player,min,max,plugin,Color.RED);
                }else{
                    player.sendMessage("Couldn't find market regions! Make sure you have created it.");
                }
            }else if(Objects.equals(strings[0], "plot")){
                Location min = RegionData.get().getLocation("market.plots."+Integer.parseInt(strings[1])+".posMin");
                Location max = RegionData.get().getLocation("market.plots."+Integer.parseInt(strings[1])+".posMax");
                if(max !=null && min !=null){
                    RegionUtils.visibleRegionBorders(player,min,max,plugin, Color.LIME);
                }else{
                    player.sendMessage("Couldn't find the plot.");
                }
            }else{
                player.sendMessage("No region in argument!");
            }

        }
        return true;
    }

}
