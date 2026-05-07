package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SetOwner extends SubCommand {

    @Override
    public String getName() {
        return "setowner";
    }

    @Override
    public String getDescription() {
        return "Force set the owner of a market plot.";
    }

    @Override
    public String getSyntax() {
        return "/market admin setowner <plotId> <player>";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        if (!player.hasPermission("market.admin")) {
            SendMessage.sendPlayerMessage(player, "&cYou do not have permission to use this command.");
            return;
        }

        if (args.length < 3) {
            SendMessage.sendPlayerMessage(player, "&cUsage: /market admin setowner <plotId> <player>");
            return;
        }

        String plotId = args[1];
        String targetName = args[2];

        if (!RegionData.get().contains("market.plots." + plotId)) {
            SendMessage.sendPlayerMessage(player, "§cPlot " + plotId + " does not exist.");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            SendMessage.sendPlayerMessage(player, "§cPlayer not found.");
            return;
        }

        RegionData.get().set("market.plots." + plotId + ".owner", target.getUniqueId().toString());
        RegionData.save();

        SendMessage.sendPlayerMessage(player, "§aSuccessfully set " + target.getName() + " as the owner of plot " + plotId);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(RegionData.get().getConfigurationSection("market.plots").getKeys(false));
        } else if (args.length == 3) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return names;
        }
        return null;
    }

    @Override
    public List<String> getAliases() {
        return null;
    }
}
