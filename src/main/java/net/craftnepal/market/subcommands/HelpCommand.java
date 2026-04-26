package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class HelpCommand extends SubCommand {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Show the market help menu.";
    }

    @Override
    public String getSyntax() {
        return "/market help";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player))
            return;
        Player player = (Player) sender;

        SendMessage.sendPlayerMessage(player, "§7--- §bMarket Commands §7---");
        SendMessage.sendPlayerMessage(player, "§e/market §7- Teleport to market spawn.");
        SendMessage.sendPlayerMessage(player,
                "§e/market plot <subcommand> §7- Manage your market plots.");
        SendMessage.sendPlayerMessage(player,
                "§e/market shops §7- View a list of all active shops.");
        SendMessage.sendPlayerMessage(player,
                "§e/market back §7- Teleport back to your previous location.");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return null;
    }
}
