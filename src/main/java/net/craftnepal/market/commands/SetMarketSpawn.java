package net.craftnepal.market.commands;

import net.craftnepal.market.files.RegionData;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetMarketSpawn implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player){
            Player p = (Player) commandSender;
            Location location = p.getLocation();
            RegionData.get().set("market.spawn", location);
            RegionData.save();
            p.sendMessage("Spawn set!");
        }
        return true;
    }
}
