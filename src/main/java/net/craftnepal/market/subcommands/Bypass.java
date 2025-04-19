package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Bypass extends SubCommand {
    public static HashMap<UUID,Boolean> bypassPlayers = new HashMap<>();

    @Override
    public String getName() {
        return "bypass";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "bypass region restrictions";
    }

    @Override
    public String getSyntax() {
        return null;
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;
            if(bypassPlayers.containsKey(player.getUniqueId())){
                bypassPlayers.remove(player.getUniqueId());
                SendMessage.sendPlayerMessage(player,"You are no longer bypassing region restrictions.");
            }else{
                bypassPlayers.put(player.getUniqueId(),true);
                SendMessage.sendPlayerMessage(player,"You are now bypassing region restrictions.");
            }
        }else{
            Bukkit.getLogger().info("You arent a player");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
