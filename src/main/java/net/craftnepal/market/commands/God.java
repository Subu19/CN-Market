package net.craftnepal.market.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class God implements CommandExecutor {


    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player){
            Player p = (Player) commandSender;
            if(p.isInvulnerable()){
                p.setInvulnerable(false);
                p.sendMessage("You are no longer a god!");
            }else{
                p.setInvulnerable(true);
                p.sendMessage("You are now a god!");
            }
        }
        return true;
    }
}
