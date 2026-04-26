package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.managers.DynamicPriceManager;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ForceUpdate extends SubCommand {

    @Override
    public String getName() {
        return "forceupdate";
    }

    @Override
    public String getDescription() {
        return "Force update dynamic prices (supply/demand).";
    }

    @Override
    public String getSyntax() {
        return "/market admin forceupdate";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!sender.hasPermission("market.admin")) {
            SendMessage.sendPlayerMessage((Player) sender, "§cYou don't have permission.");
            return;
        }

        SendMessage.sendPlayerMessage((Player) sender, "§aForcing dynamic price update...");
        DynamicPriceManager.triggerDailyUpdate();
        SendMessage.sendPlayerMessage((Player) sender, "§aUpdate complete.");
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
