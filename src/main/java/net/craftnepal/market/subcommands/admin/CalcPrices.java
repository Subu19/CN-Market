package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.utils.PriceCalculator;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CalcPrices extends SubCommand {

    @Override
    public String getName() {
        return "calcprices";
    }

    @Override
    public String getDescription() {
        return "Automatically calculate missing item prices based on crafting recipes.";
    }

    @Override
    public String getSyntax() {
        return "/market admin calcprices";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("market.admin")) {
            SendMessage.sendPlayerMessage((Player) sender, "§cYou don't have permission to use this command.");
            return;
        }

        sender.sendMessage("§eCalculating prices based on recipes... Please wait.");
        
        int updated = PriceCalculator.calculateMissingPrices();
        
        if (sender instanceof Player) {
            SendMessage.sendPlayerMessage((Player) sender, "§aCalculation complete! Updated §f" + updated + " §aitem prices in price.yml.");
        } else {
            sender.sendMessage("Calculation complete! Updated " + updated + " item prices in price.yml.");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("calculateprices");
    }
}
