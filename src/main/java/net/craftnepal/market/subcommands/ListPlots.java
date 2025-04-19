package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.colors.ColorTranslator;
import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ListPlots extends SubCommand {
    @Override
    public String getName() {
        return "listplots";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "List all  the existing plots.";
    }

    @Override
    public String getSyntax() {
        return "/amarket listplots";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;
            ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
            if(plots != null){
                SendMessage.sendPlayerMessage(player, "Plots are listed below:");
                for(String plot: plots.getKeys(false)){
                    String owner = RegionData.get().getString("market.plots."+plot+".owner");
                    if(owner == null){
                        owner = "Unclaimed";
                    }else{
                        OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(owner));
                        owner = p.getName();
                    }
                    player.sendMessage(ColorTranslator.translateColorCodes("&ePlotNo: &a"+plot+" &e------ Owner: &a"+owner));
                }
            }else{
                SendMessage.sendPlayerMessage(player,"There are no plots to list them.");
            }
        }else{
            Bukkit.getLogger().info("You arent a player!");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
