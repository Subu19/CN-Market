package net.craftnepal.market.subcommands;

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
        if(commandSender instanceof  Player){
            Player player = (Player) commandSender;
            Location location = player.getLocation();
            ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
            if(plots != null){
                String selectedPlot = null;

                //get selected plot
                for(String plot: plots.getKeys(false)){
                    Location min = RegionData.get().getLocation("market.plots."+plot+".posMin");
                    Location max = RegionData.get().getLocation("market.plots."+plot+".posMax");
                    if(min != null && max != null){
                        if(RegionUtils.isLocationInsideRegion(location,min,max)){
                            selectedPlot = plot;
                        }
                    }else{
                        SendMessage.sendPlayerMessage(player,"Something went wrong!");
                    }
                }

                if(selectedPlot != null){
                    String owner = RegionData.get().getString("market.plots."+selectedPlot+".owner");                    if(owner == null || owner.isEmpty()){
                        PlotUtils.setPlotOwner(selectedPlot, player.getUniqueId().toString());
                        SendMessage.sendPlayerMessage(player,"&aYou successfully claimed plot: "+ selectedPlot);
                        player.playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1,1);
                    }else if(owner.equals(player.getUniqueId().toString())){
                        SendMessage.sendPlayerMessage(player,"&aYou already own this plot : "+ selectedPlot);
                    }else{
                        SendMessage.sendPlayerMessage(player,"&aIt is claimed by someone else");
                    }

                }else{
                    SendMessage.sendPlayerMessage(player,"No plot found at your location..");
                    player.playSound(location,Sound.ITEM_SHIELD_BREAK,1,1);

                }

            }else{
                SendMessage.sendPlayerMessage(player,"No plots found.");
            }
        }else{
            Bukkit.getLogger().info("You are not a player");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
