package net.craftnepal.market.subcommands.plot;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Market;
import net.craftnepal.market.managers.DatabaseManager;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.RegionUtils;
import net.craftnepal.market.utils.SendMessage;
import net.craftnepal.market.utils.TeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlotTeleport extends SubCommand {
    @Override
    public String getName() {
        return "visit";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Teleport to a certain plot";
    }

    @Override
    public String getSyntax() {
        return "/market visit <playername/number>";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;

            if (!player.hasPermission("market.plot.teleport")) {
                SendMessage.sendPlayerMessage(player, "&cYou do not have permission to use this command.");
                return;
            }

            if (strings.length < 2) {
                SendMessage.sendPlayerMessage(player, "&cUsage: /market visit <playername/plotID>");
                return;
            }
            String target = strings[1];
            List<String> plotIds = DatabaseManager.getAllPlotIds();
            
            if(!plotIds.isEmpty()){
                // First check if target is a plot ID
                if (plotIds.contains(target)) {
                    teleportToPlot(player, target);
                    return;
                }

                // Otherwise search by player name
                for(String plot: plotIds){
                    String ownerUUID = PlotUtils.getPlotOwner(plot);
                    if (ownerUUID == null || ownerUUID.isEmpty()) continue;

                    OfflinePlayer plotOwner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
                    String plotOwnerName = plotOwner.getName();
                    
                    if(plotOwnerName != null && plotOwnerName.equalsIgnoreCase(target)){
                        teleportToPlot(player, plot);
                        return;
                    }
                }
                SendMessage.sendPlayerMessage(player, "&cCould not find a plot owned by '" + target + "' or with ID '" + target + "'.");
            }else{
                SendMessage.sendPlayerMessage(player,"&cNo plots have been registered yet.");
            }
        }else{
            Bukkit.getLogger().info("You are not a player!");
        }
    }

    private void teleportToPlot(Player player, String plotId) {
        Location min = PlotUtils.getPlotPosMin(plotId);
        Location max = PlotUtils.getPlotPosMax(plotId);
        if (min == null || max == null) {
            SendMessage.sendPlayerMessage(player, "&cError: Plot boundaries not found for " + plotId);
            return;
        }

        Location plotSpawn = PlotUtils.getPlotSpawn(plotId);
        Location tpLocation;

        if (plotSpawn != null) {
            tpLocation = plotSpawn;
        } else {
            tpLocation = new Location(
                    min.getWorld(),
                    (min.getX() + max.getX()) / 2,
                    min.getY() + 2,
                    (min.getZ() + max.getZ()) / 2
            );
        }

        Location origin = player.getLocation();
        SendMessage.sendPlayerMessage(player, "&eTeleporting to plot " + plotId + " in 5 seconds.. don't move!");
        TeleportUtils.scheduleTeleport(player, tpLocation, () -> {
            if (!net.craftnepal.market.utils.MarketUtils.isInMarketArea(origin)) {
                net.craftnepal.market.utils.PlayerUtils.saveLastLocation(player, origin);
            }
            RegionUtils.visibleRegionBorders(player, min, max, Market.getPlugin(), Color.PURPLE, 30);
        });
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        List<String> players = new ArrayList<>();
        for(Player p: Market.getPlugin().getServer().getOnlinePlayers()){
            players.add(p.getName());
        }
        return players;
    }
}
