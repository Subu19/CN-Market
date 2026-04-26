package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Listeners.RegionSelection;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SelectionMode extends SubCommand {
    @Override
    public String getName() {
        return "selectionmode";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Enable selection move for market or plots";
    }

    @Override
    public String getSyntax() {
        return "/amarket selectionmove <mode>";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof Player)) {
            Bukkit.getLogger().info("This command can only be run by a player!");
            return;
        }
        Player player = (Player) commandSender;
        if (!player.hasPermission("market.admin")) {
            SendMessage.sendPlayerMessage(player, "§cYou don't have permission to use this command.");
            return;
        }
        if (strings.length < 2) {
            player.sendMessage("§cUsage: /market admin selectionmode <plot>");
            return;
        }
        UUID uuid = player.getUniqueId();
        if (strings[1].equals("plot")) {
            RegionSelection.SelectionMode mode = RegionSelection.SelectionMode.MARKET_PLOT;
            if (RegionSelection.isInSelectionMode(uuid)) {
                RegionSelection.removeSelectionModePlayer(uuid);
                player.sendMessage("§aSelection mode disabled!");
            } else {
                RegionSelection.addSelectionModePlayer(uuid, mode);
                player.sendMessage("§aSelection mode enabled! Use a Stick to select a region.");
            }
        } else {
            player.sendMessage("§cUnknown mode. Use: plot");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        List<String> autocomplete = new ArrayList<>();
        autocomplete.add("plot");
        return autocomplete ;
    }
}
