package net.craftnepal.market.subcommands.admin;

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
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            if (!player.hasPermission("market.admin")) {
                SendMessage.sendPlayerMessage(player, "§cYou don't have permission to use this command.");
                return;
            }
            if (bypassPlayers.containsKey(player.getUniqueId())) {
                bypassPlayers.remove(player.getUniqueId());
                SendMessage.sendPlayerMessage(player, "§aYou are no longer bypassing region restrictions.");
                SendMessage.sendPlayerMessage(player, "§7Admin Shop mode §cDISABLED§7.");
            } else {
                bypassPlayers.put(player.getUniqueId(), true);
                SendMessage.sendPlayerMessage(player, "§aYou are now bypassing region restrictions.");
                SendMessage.sendPlayerMessage(player, "§7Admin Shop mode §2ENABLED§7. Shops you create will be infinite.");
            }
        } else {
            Bukkit.getLogger().info("You aren't a player");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
