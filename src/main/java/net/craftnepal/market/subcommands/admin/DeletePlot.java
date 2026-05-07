package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class DeletePlot extends SubCommand {
    @Override
    public String getName() {
        return "deleteplot";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Delete a plot";
    }

    @Override
    public String getSyntax() {
        return "/market admin deleteplot <plotId>";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            if (!player.hasPermission("market.admin")) {
                SendMessage.sendPlayerMessage(player, "&cYou do not have permission to use this command.");
                return;
            }
            if (strings.length < 2) {
                SendMessage.sendPlayerMessage(player, "&cUsage: /market admin deleteplot <plotId>");
                return;
            }
            String plot = strings[1];
            if (RegionData.get().get("market.plots." + plot) != null) {
                // Remove all shops in the plot
                org.bukkit.configuration.ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plot + ".shops");
                if (shops != null) {
                    for (String shopId : shops.getKeys(false)) {
                        net.craftnepal.market.utils.ShopUtils.removeShop(plot, shopId);
                    }
                }

                // Remove the entire plot data from config
                RegionData.get().set("market.plots." + plot, null);
                RegionData.save();

                SendMessage.sendPlayerMessage(player, "&aDeleted plot: &b" + plot);
            } else {
                SendMessage.sendPlayerMessage(player, "§cPlot not found: " + plot);
            }
        } else {
            Bukkit.getLogger().info("You are not a player");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
