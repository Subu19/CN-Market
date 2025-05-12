package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Listeners.Movement;
import net.craftnepal.market.files.LocationData;
import net.craftnepal.market.utils.PlayerUtils;
import net.craftnepal.market.utils.RegionUtils;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class Back extends SubCommand {
    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Teleport back to last location";
    }

    @Override
    public String getSyntax() {
        return "/market leave";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;
            //Check if the player is inside market
            if(Movement.isPlayerInMarket(player.getUniqueId())){
                Bukkit.getLogger().info("player is in market");
                Location lastLocation = PlayerUtils.getLastLocation(player);
                if(lastLocation != null){
                    player.teleport(lastLocation);
                    Movement.getPlayersInMarket().remove(player.getUniqueId());
                    //toggle fly
                    Movement.checkAndToggle(player,false);

                    SendMessage.sendPlayerMessage(player,"You left the Market!");
                    //remove user from lastlocation database
                    PlayerUtils.clearLastLocation(player);
                }else{
                    SendMessage.sendPlayerMessage(player,"Last location not Found!");
                }
            }else{
                SendMessage.sendPlayerMessage(player,"You are not in the market!");
            }
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
