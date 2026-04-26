package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class NestedCommand extends SubCommand {

    private final List<SubCommand> subCommands = new ArrayList<>();

    /**
     * Override this in AdminCommand to require market.admin for the whole group.
     * Return null to allow anyone.
     */
    public String getRequiredPermission() {
        return null;
    }

    public void registerSubCommand(SubCommand subCommand) {
        subCommands.add(subCommand);
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        // Check group-level permission
        String perm = getRequiredPermission();
        if (perm != null && !player.hasPermission(perm)) {
            SendMessage.sendPlayerMessage(player, "§cYou don't have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            // show help — only list commands the player can see
            SendMessage.sendPlayerMessage(player, "§7--- §b" + getName() + " Commands §7---");
            for (SubCommand subCommand : subCommands) {
                if (subCommand.getSyntax() != null) {
                    SendMessage.sendPlayerMessage(player, "§e" + subCommand.getSyntax() + " §7- " + subCommand.getDescription());
                }
            }
            return;
        }

        String subCommandName = args[1];
        for (SubCommand subCommand : subCommands) {
            if (subCommand.getName().equalsIgnoreCase(subCommandName) ||
               (subCommand.getAliases() != null && subCommand.getAliases().contains(subCommandName))) {

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
        // If the whole group is gated by a permission, return nothing for players without it
        String perm = getRequiredPermission();
        if (perm != null && !player.hasPermission(perm)) {
            return new ArrayList<>();
        }

        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (SubCommand subCommand : subCommands) {
                // Here we could check for per-subcommand permissions if we had them,
                // but for now, we rely on the group-level check above.
                // However, we can add a check here if we want more granularity later.
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
