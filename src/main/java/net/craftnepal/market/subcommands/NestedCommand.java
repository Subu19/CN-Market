package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class NestedCommand extends SubCommand {

    private final List<SubCommand> subCommands = new ArrayList<>();

    public void registerSubCommand(SubCommand subCommand) {
        subCommands.add(subCommand);
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        if (args.length < 2) {
            // show help for nested command
            SendMessage.sendPlayerMessage(player, "§7--- §b" + getName() + " Commands §7---");
            for (SubCommand subCommand : subCommands) {
                SendMessage.sendPlayerMessage(player, "§e" + subCommand.getSyntax() + " §7- " + subCommand.getDescription());
            }
            return;
        }

        String subCommandName = args[1];
        for (SubCommand subCommand : subCommands) {
            if (subCommand.getName().equalsIgnoreCase(subCommandName) || 
               (subCommand.getAliases() != null && subCommand.getAliases().contains(subCommandName))) {
                
                // Create a proxy args array that shifts everything left by 1
                // Original: /market plot claim
                // args: ["plot", "claim"]
                // Proxy: ["claim"]
                String[] proxyArgs = new String[args.length - 1];
                System.arraycopy(args, 1, proxyArgs, 0, args.length - 1);
                
                subCommand.perform(sender, proxyArgs);
                return;
            }
        }

        SendMessage.sendPlayerMessage(player, "§cUnknown subcommand. Type /market " + getName() + " for help.");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (SubCommand subCommand : subCommands) {
                names.add(subCommand.getName());
            }
            return names;
        } else if (args.length > 2) {
            String subCommandName = args[1];
            for (SubCommand subCommand : subCommands) {
                if (subCommand.getName().equalsIgnoreCase(subCommandName) || 
                   (subCommand.getAliases() != null && subCommand.getAliases().contains(subCommandName))) {
                    
                    String[] proxyArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, proxyArgs, 0, args.length - 1);
                    return subCommand.getSubcommandArguments(player, proxyArgs);
                }
            }
        }
        return null;
    }
}
