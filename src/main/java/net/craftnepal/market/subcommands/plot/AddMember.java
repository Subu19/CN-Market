package net.craftnepal.market.subcommands.plot;

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
import java.util.UUID;

public class AddMember extends SubCommand {

    @Override
    public String getName() {
        return "addmember";
    }

    @Override
    public String getDescription() {
        return "Add a friend to your market plot.";
    }

    @Override
    public String getSyntax() {
        return "/market addmember <player>";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        if (args.length < 2) {
            SendMessage.sendPlayerMessage(player, "§cUsage: /market addmember <player>");
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            SendMessage.sendPlayerMessage(player, "§cPlayer not found.");
            return;
        }

        String plotId = PlotUtils.getPlotIdByLocation(player.getLocation());

        if (plotId == null) {
            SendMessage.sendPlayerMessage(player, "§cYou must be standing inside a plot to add a member.");
            return;
        }

        String owner = PlotUtils.getPlotOwner(plotId);
        if (owner == null || !owner.equals(player.getUniqueId().toString())) {
            SendMessage.sendPlayerMessage(player, "§cYou must be the owner of the plot to add members.");
            return;
        }

        String targetUUID = target.getUniqueId().toString();

        if (targetUUID.equals(owner)) {
            SendMessage.sendPlayerMessage(player, "§cYou cannot add yourself as a member.");
            return;
        }

        List<String> members = RegionData.get().getStringList("market.plots." + plotId + ".members");
        if (members.contains(targetUUID)) {
            SendMessage.sendPlayerMessage(player, "§cThat player is already a member of this plot.");
            return;
        }

        members.add(targetUUID);
        RegionData.get().set("market.plots." + plotId + ".members", members);
        RegionData.save();

        SendMessage.sendPlayerMessage(player, "§aSuccessfully added " + target.getName() + " to your plot.");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
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
