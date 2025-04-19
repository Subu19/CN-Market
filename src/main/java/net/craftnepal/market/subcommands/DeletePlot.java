package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class DeletePlot extends SubCommand {
    @Override
    public String getName() {
        return "deleteplot";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Delete a plot";
    }

    @Override
    public String getSyntax() {
        return "/amarket deleteplot <plot>";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;
            String plot = strings[1];
            if(plot.isEmpty() || plot ==null){
                SendMessage.sendPlayerMessage(player,"Please provide plot number.");
            }else{
                if(RegionData.get().get("market.plots."+plot) != null){
                    RegionData.get().set("market.plots."+plot, null);
                    RegionData.save();
                    SendMessage.sendPlayerMessage(player,"Deleted plot no: &b"+plot);
                }else{
                    SendMessage.sendPlayerMessage(player,"Please provide valid plot number.");
                }

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
