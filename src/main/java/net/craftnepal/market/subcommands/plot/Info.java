package net.craftnepal.market.subcommands.plot;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class Info extends SubCommand {

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "View information about the plot you are standing in.";
    }

    @Override
    public String getSyntax() {
        return "/market info";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        String plotId = PlotUtils.getPlotIdByLocation(player.getLocation());

        if (plotId == null) {
            SendMessage.sendPlayerMessage(player, "§cYou are not standing inside any market plot.");
            return;
        }

        String ownerUUID = PlotUtils.getPlotOwner(plotId);
        String ownerName = "Unclaimed";
        
        if (ownerUUID != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
            if (owner.getName() != null) {
                ownerName = owner.getName();
            }
        }

        List<String> members = RegionData.get().getStringList("market.plots." + plotId + ".members");
        StringBuilder memberNames = new StringBuilder();
        
        if (!members.isEmpty()) {
            for (String uuidStr : members) {
                OfflinePlayer member = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                if (member.getName() != null) {
                    memberNames.append(member.getName()).append(", ");
                }
            }
            if (memberNames.length() > 0) {
                memberNames.setLength(memberNames.length() - 2); // remove trailing comma
            }
        } else {
            memberNames.append("None");
        }

        SendMessage.sendPlayerMessage(player, "§7=============================");
        SendMessage.sendPlayerMessage(player, "§aPlot Info: §b" + plotId);
        SendMessage.sendPlayerMessage(player, "§7Owner: §e" + ownerName);
        SendMessage.sendPlayerMessage(player, "§7Members: §d" + memberNames.toString());
        SendMessage.sendPlayerMessage(player, "§7=============================");
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
