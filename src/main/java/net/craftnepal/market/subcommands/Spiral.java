package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Entities.SpiralEntity;
import net.craftnepal.market.Market;
import net.craftnepal.market.utils.PrintParticles;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Spiral extends SubCommand {
    static Double angle = 0D;
    static HashMap<UUID,Integer> spiralTasks = new HashMap<>();
    @Override
    public String getName() {
        return "spiral";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "spawn spiral at your location";
    }

    @Override
    public String getSyntax() {
        return "/bd spiral <radius>";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if(commandSender instanceof Player){
            Player p = (Player) commandSender;
            if(spiralTasks.containsKey(p.getUniqueId())){
                Bukkit.getScheduler().cancelTask(spiralTasks.get(p.getUniqueId()));
                for (int letter : PrintParticles.letters){
                    Bukkit.getScheduler().cancelTask(letter);
                }
                SendMessage.sendPlayerMessage(p,"Spiral stopped!");
                spiralTasks.remove(p.getUniqueId());
            }else{

                SpiralEntity spiral = new SpiralEntity(Integer.parseInt(strings[1]),p.getLocation(),p);
                spiralTasks.put(p.getUniqueId(),Bukkit.getScheduler().scheduleSyncRepeatingTask(Market.getPlugin(),()->{
                    spiral.update();
                },1,1L));
                SendMessage.sendPlayerMessage(p,"Spiral spawnned!");
            }
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
