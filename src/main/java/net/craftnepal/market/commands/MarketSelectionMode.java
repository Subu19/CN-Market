package net.craftnepal.market.commands;


import net.craftnepal.market.Listeners.RegionSelection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MarketSelectionMode implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;
            UUID uuid = player.getUniqueId();
      if(strings.length>0){
          if(strings[0].equals("plot")){
              RegionSelection.SelectionMode mode = RegionSelection.SelectionMode.MARKET_PLOT;

              if(RegionSelection.isInSelectionMode(uuid)){
                  RegionSelection.removeSelectionModePlayer(uuid);
                  player.sendMessage("Selection mode disabled!");
              }else{
                  RegionSelection.addSelectionModePlayer(uuid,mode);
                  player.sendMessage("Selection mode enabled! Please use Stick to select region");
              }
          }else{
              player.sendMessage("Please pass which region you want to make (plot)");
          }
      }else{
          player.sendMessage("Please specify what region you are creating! (plot)");
      }

        }
        return true;
    }
}
