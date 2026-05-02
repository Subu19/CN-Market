package net.craftnepal.market.subcommands.plot;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.SendMessage;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class Unclaim extends SubCommand {
    @Override
    public String getName() {
        return "unclaim";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Unclaim your market plot and remove all shops within it.";
    }

    @Override
    public String getSyntax() {
        return "/market plot unclaim";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;

            if (!player.hasPermission("market.plot.unclaim")) {
                SendMessage.sendPlayerMessage(player, "&cYou do not have permission to unclaim plots.");
                return;
            }

            String plotId = PlotUtils.getPlotIdByLocation(player.getLocation());
            if (plotId == null) {
                SendMessage.sendPlayerMessage(player, "&cYou are not standing in a plot.");
                return;
            }

            String owner = PlotUtils.getPlotOwner(plotId);
            if (owner == null || !owner.equals(player.getUniqueId().toString())) {
                SendMessage.sendPlayerMessage(player, "&cYou do not own this plot.");
                return;
            }

            // Remove all shops in the plot
            ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops");
            if (shops != null) {
                for (String shopId : shops.getKeys(false)) {
                    ShopUtils.removeShop(plotId, shopId);
                }
            }

            // Remove members
            RegionData.get().set("market.plots." + plotId + ".members", null);
            
            // Unset owner (this also saves and handles cleanup in PlotUtils)
            PlotUtils.setPlotOwner(plotId, null);

            SendMessage.sendPlayerMessage(player, "&aYou have successfully unclaimed plot: " + plotId);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

        } else {
            Bukkit.getLogger().info("You are not a player");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
