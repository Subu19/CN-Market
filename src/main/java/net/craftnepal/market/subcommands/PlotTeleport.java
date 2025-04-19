package net.craftnepal.market.subcommands;

import jdk.vm.ci.code.site.Mark;
import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.RegionUtils;
import net.craftnepal.market.utils.SendMessage;
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
        if(commandSender instanceof  Player){
            Player player = (Player) commandSender;
            String pp = strings[1];
            ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
            if(plots != null){
                for(String plot: plots.getKeys(false)){
                    String owner = RegionData.get().getString("market.plots."+plot+".owner");
                    //if owner exists
                    assert owner != null;
                    OfflinePlayer plotOwner = Bukkit.getOfflinePlayer(UUID.fromString(owner));
                    String plotOwnerName = plotOwner.getName();
                    assert plotOwnerName != null;
                    if(plotOwnerName.equalsIgnoreCase(pp)){
                        Location min = RegionData.get().getLocation("market.plots."+plot+".posMin");
                        Location max = RegionData.get().getLocation("market.plots."+plot+".posMax");
                        Location tpLocation = new Location(
                                player.getWorld(),
                                (min.getX()+max.getX())/2,
                                min.getY()+2,
                                min.getZ()
                        );
                        SendMessage.sendPlayerMessage(player,"Teleporting in 2 seconds..");
                        Bukkit.getScheduler().scheduleSyncDelayedTask(Market.getPlugin(),()->{
                            player.teleport(tpLocation);
                            RegionUtils.visibleRegionBorders(player,min,max,Market.getPlugin(), Color.PURPLE,30);
                        },50L);
                        return;
                    }
                }
                SendMessage.sendPlayerMessage(player,"That player doesn't own any shop yet.");
            }else{
                SendMessage.sendPlayerMessage(player,"No plot found!");
            }
        }else{
            Bukkit.getLogger().info("You are not a player!");
        }
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
