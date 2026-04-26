package net.craftnepal.market.subcommands.plot;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.DisplayUtils;
import net.craftnepal.market.utils.LocationUtils;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class RemoveShop extends SubCommand {
    @Override
    public String getName() {
        return "removeshop";
    }

    @Override
    public String getDescription() {
        return "Internal command to remove a shop.";
    }

    @Override
    public String getSyntax() {
        return "/market plot removeshop <plotId> <shopId>";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        if (args.length < 3) {
            SendMessage.sendPlayerMessage(player, "§cInvalid usage.");
            return;
        }

        String plotId = args[1];
        String shopId = args[2];

        String basePath = "market.plots." + plotId + ".shops." + shopId;
        if (!RegionData.get().contains(basePath)) {
            SendMessage.sendPlayerMessage(player, "§cThis shop no longer exists.");
            return;
        }

        String owner = RegionData.get().getString(basePath + ".owner");
        if (owner == null) return;

        if (player.hasPermission("market.admin") || owner.equals(player.getUniqueId().toString())) {
            RegionData.get().set(basePath, null);
            RegionData.save();
            SendMessage.sendPlayerMessage(player, "§aShop removed successfully.");
            DisplayUtils.getInstance().removeDisplayPair(plotId, shopId);
        } else {
            SendMessage.sendPlayerMessage(player, "§cYou do not have permission to remove this shop.");
        }
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
