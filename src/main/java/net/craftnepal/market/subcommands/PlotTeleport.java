package net.craftnepal.market.subcommands;


import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.RegionUtils;
import net.craftnepal.market.utils.SendMessage;
import net.craftnepal.market.utils.TeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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
            if (strings.length < 2) {
                SendMessage.sendPlayerMessage(player, "&cUsage: /market visit <playername/plotID>");
                return;
            }
            String target = strings[1];
            ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
            
            if(plots != null){
                // First check if target is a plot ID
                if (plots.contains(target)) {
                    teleportToPlot(player, target);
                    return;
                }

                // Otherwise search by player name
                for(String plot: plots.getKeys(false)){
                    String ownerUUID = RegionData.get().getString("market.plots."+plot+".owner");
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
        Location min = RegionData.get().getLocation("market.plots." + plotId + ".posMin");
        Location max = RegionData.get().getLocation("market.plots." + plotId + ".posMax");
        if (min == null || max == null) {
            SendMessage.sendPlayerMessage(player, "&cError: Plot boundaries not found for " + plotId);
            return;
        }

        Location plotSpawn = RegionData.get().getLocation("market.plots." + plotId + ".spawn");
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

        SendMessage.sendPlayerMessage(player, "&eTeleporting to plot " + plotId + " in 5 seconds.. don't move!");
        TeleportUtils.scheduleTeleport(player, tpLocation, () -> {
            RegionUtils.visibleRegionBorders(player, min, max, Market.getPlugin(), Color.PURPLE, 30);
        });
    }


    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        List<String> players = new ArrayList<>();
        for(Player p: Market.getPlugin().getServer().getOnlinePlayers()){
            players.add(p.getName());
        }
        return  players;
    }
}
