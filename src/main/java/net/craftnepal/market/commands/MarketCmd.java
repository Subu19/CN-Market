package net.craftnepal.market.commands;

import net.craftnepal.market.files.RegionData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MarketCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player){
            Player p = (Player) commandSender;
            Location location = RegionData.get().getLocation("market.spawn");
            if(location !=null){
                p.teleport(location);
                p.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT,1,1);
                p.spawnParticle(Particle.TOTEM,location,50);
            }else{
                p.sendMessage("Market spawn not set!");
            }
        }
        return true;
    }
}
