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

        if (!player.hasPermission("market.plot.info")) {
            SendMessage.sendPlayerMessage(player, "§cYou do not have permission to use this command.");
            return;
        }

        String plotId = PlotUtils.getPlotIdByLocation(player.getLocation());

        if (plotId == null) {
            SendMessage.sendPlayerMessage(player, "§cYou are not standing inside any market plot.");
            return;
        }

        // Friendly plot display name
        String ownerUUID = PlotUtils.getPlotOwner(plotId);
        String ownerName = "Unclaimed";
        String friendlyName;
        if (ownerUUID != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
            ownerName = owner.getName() != null ? owner.getName() : "Unknown";
            friendlyName = ownerName + "'s Plot";
        } else {
            friendlyName = "Unclaimed Plot";
        }

        // Count shops
        org.bukkit.configuration.ConfigurationSection shopSection =
                RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops");
        int shopCount = shopSection != null ? shopSection.getKeys(false).size() : 0;

        SendMessage.sendPlayerMessage(player, "§7=============================");
        SendMessage.sendPlayerMessage(player, "§a§l" + friendlyName);
        SendMessage.sendPlayerMessage(player, "§7Owner: §e" + ownerName);
        SendMessage.sendPlayerMessage(player, "§7Shops: §b" + shopCount);
        // Build member names list
        List<String> members = RegionData.get().getStringList("market.plots." + plotId + ".members");
        StringBuilder memberNames = new StringBuilder();
        if (!members.isEmpty()) {
            for (String uuidStr : members) {
                try {
                    OfflinePlayer member = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                    if (member.getName() != null) memberNames.append(member.getName()).append(", ");
                } catch (IllegalArgumentException ignored) {}
            }
            if (memberNames.length() > 2) memberNames.setLength(memberNames.length() - 2);
        } else {
            memberNames.append("None");
        }

        SendMessage.sendPlayerMessage(player, "§7Members: §d" + memberNames.toString());
        if (player.hasPermission("market.admin")) {
            SendMessage.sendPlayerMessage(player, "§8[Admin] Internal ID: §7" + plotId);
        }
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
