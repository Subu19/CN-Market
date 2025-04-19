package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Listeners.RegionSelection;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SelectionMode extends SubCommand {
    @Override
    public String getName() {
        return "selectionmode";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Enable selection move for market or plots";
    }

    @Override
    public String getSyntax() {
        return "/amarket selectionmove <mode>";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;
            UUID uuid = player.getUniqueId();
            if(strings[1].equals("market") || strings[1].equals("plot")){

                RegionSelection.SelectionMode mode = null;
                if(strings[1].equals("market"))
                    mode = RegionSelection.SelectionMode.MARKET;
                else mode = RegionSelection.SelectionMode.MARKET_PLOT;

                if(RegionSelection.isInSelectionMode(uuid)){
                    RegionSelection.removeSelectionModePlayer(uuid);
                    player.sendMessage("Selection mode disabled!");
                }else{
                    RegionSelection.addSelectionModePlayer(uuid,mode);
                    player.sendMessage("Selection mode enabled! Please use Stick to select region");
                }
            }else{
                player.sendMessage("Please pass which region you want to make (market/plot)");
            }

        }else{
            Bukkit.getLogger().info("This command can only be run by player!");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        List<String> autocomplete = new ArrayList<>();
        autocomplete.add("market");
        autocomplete.add("plot");
        return autocomplete ;
    }
}
