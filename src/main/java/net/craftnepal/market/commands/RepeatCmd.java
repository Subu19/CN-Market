package net.craftnepal.market.commands;

import net.craftnepal.market.files.RegionData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class RepeatCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player){
            Player p = (Player) commandSender;
            p.sendMessage(Objects.requireNonNull(RegionData.get().getString("market")));
        }
        return true;
    }
}
