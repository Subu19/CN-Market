package net.craftnepal.market.commands;

import net.craftnepal.market.Market;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MarketCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player){
            Player p = (Player) commandSender;
            World marketWorld = Market.getPlugin().getMarketWorld();
            if(marketWorld != null){
                Location location = marketWorld.getSpawnLocation();
                p.teleport(location);
                p.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT,1,1);
                p.spawnParticle(Particle.TOTEM_OF_UNDYING,location,50);
            }else{
                p.sendMessage("Market world not loaded!");
            }
        }
        return true;
    }

}
