package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Listeners.Movement;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.LocationData;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class Spawn extends SubCommand {
    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Teleport to market";
    }

    @Override
    public String getSyntax() {
        return "/market spawn";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;
            Location spawn = RegionData.get().getLocation("market.spawn");

            if(spawn == null )
                return;
            if(Movement.getPlayersInMarket().containsKey(player.getUniqueId())){
                SendMessage.sendPlayerMessage(player,"You are already in market!");
            }else{
                SendMessage.sendPlayerMessage(player,"Teleporting to market in 5 seconds");
                TeleportUtils.scheduleTeleport(player, spawn, () -> {
                    PlayerUtils.saveLastLocation(player);
                    Movement.getPlayersInMarket().put(player.getUniqueId(), true);
                    SendMessage.sendPlayerMessage(player, "&aWelcome to market!");
                },50L);
            }
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
