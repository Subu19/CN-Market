package net.craftnepal.market.Listeners;

import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.subcommands.Bypass;
import net.craftnepal.market.utils.RegionUtils;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class MarketRegionProtection implements Listener {
    @EventHandler
    public void onPlayerIteration(PlayerInteractEvent e){
        Player player = e.getPlayer();
        //market region
        Location min = RegionData.get().getLocation("market.posMin");
        Location max = RegionData.get().getLocation("market.posMax");

        //check if the interation is inside any of the plots or market
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if(plots != null && e.getClickedBlock() != null){
            for(String plot: plots.getKeys(false)){
                //plot region
                Location minPos = RegionData.get().getLocation("market.plots."+plot+".posMin");
                Location maxPos = RegionData.get().getLocation("market.plots."+plot+".posMax");
                //check if interaction is inside plot
                if(RegionUtils.isLocationInsideRegion(e.getClickedBlock().getLocation(),minPos,maxPos)){
                    String owner = RegionData.get().getString("market.plots."+plot+".owner");
                    //check if its a owner
                    if(owner != null && !player.getUniqueId().toString().equals(owner) && !Bypass.bypassPlayers.containsKey(player.getUniqueId())){
                        e.setCancelled(true);
                        SendMessage.sendPlayerMessage(player,"&cThis belongs to someone else.");
                    } else if ((owner == null || owner.isEmpty()) && !Bypass.bypassPlayers.containsKey(player.getUniqueId())) {
                        e.setCancelled(true);
                        SendMessage.sendPlayerMessage(player,"&cThis is unclaimed area and protected from grief.");
                    }
                    //stop event method
                    return;
                }

            }//loop ending
        }
        //check if interaction is inside market region
        if(min != null && max != null && e.getClickedBlock() != null){
            //check if interaction is happening inside market
            if(RegionUtils.isLocationInsideRegion(e.getClickedBlock().getLocation(),min,max) && !Bypass.bypassPlayers.containsKey(player.getUniqueId())){
                e.setCancelled(true);
                SendMessage.sendPlayerMessage(player,"&cThis is not your property.");
                return;
            }
        }
    }
    @EventHandler
    public void onPlayerIteration(BlockBreakEvent e){
        Player player = e.getPlayer();

        //market region
        Location min = RegionData.get().getLocation("market.posMin");
        Location max = RegionData.get().getLocation("market.posMax");

        //check if the interation is inside any of the plots or market
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if(plots != null){
            for(String plot: plots.getKeys(false)){
                //plot region
                Location minPos = RegionData.get().getLocation("market.plots."+plot+".posMin");
                Location maxPos = RegionData.get().getLocation("market.plots."+plot+".posMax");
                //check if interaction is inside plot
                if(RegionUtils.isLocationInsideRegion(e.getBlock().getLocation(),minPos,maxPos)){
                    String owner = RegionData.get().getString("market.plots."+plot+".owner");
                    //check if its a owner
                    if(owner != null && !player.getUniqueId().toString().equals(owner) && !Bypass.bypassPlayers.containsKey(player.getUniqueId())){
                        e.setCancelled(true);
                        SendMessage.sendPlayerMessage(player,"&cThis belongs to someone else.");
                    } else if ((owner == null || owner.isEmpty()) && !Bypass.bypassPlayers.containsKey(player.getUniqueId())) {
                        e.setCancelled(true);
                        SendMessage.sendPlayerMessage(player,"&cThis is unclaimed area and protected from grief.");
                    }
                    //stop event method
                    return;
                }

            }//loop ending
        }
        //check if interaction is inside market region
        if(min != null && max != null ){
            //check if interaction is happening inside market
            if(RegionUtils.isLocationInsideRegion(e.getBlock().getLocation(),min,max) && !Bypass.bypassPlayers.containsKey(player.getUniqueId())){
                e.setCancelled(true);
                SendMessage.sendPlayerMessage(player,"&cThis is not your property.");
                return;
            }
        }
    }


    @EventHandler
    public void onPlayerIteration(BlockPlaceEvent e){
        Player player = e.getPlayer();

        //market region
        Location min = RegionData.get().getLocation("market.posMin");
        Location max = RegionData.get().getLocation("market.posMax");

        //check if the interation is inside any of the plots or market
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if(plots != null){
            for(String plot: plots.getKeys(false)){
                //plot region
                Location minPos = RegionData.get().getLocation("market.plots."+plot+".posMin");
                Location maxPos = RegionData.get().getLocation("market.plots."+plot+".posMax");
                //check if interaction is inside plot
                if(RegionUtils.isLocationInsideRegion(e.getBlock().getLocation(),minPos,maxPos)){
                    String owner = RegionData.get().getString("market.plots."+plot+".owner");
                    //check if its a owner
                    if(owner != null && !player.getUniqueId().toString().equals(owner) && !Bypass.bypassPlayers.containsKey(player.getUniqueId())){
                        e.setCancelled(true);
                        SendMessage.sendPlayerMessage(player,"&cThis belongs to someone else.");
                    } else if ((owner == null || owner.isEmpty()) && !Bypass.bypassPlayers.containsKey(player.getUniqueId())) {
                        e.setCancelled(true);
                        SendMessage.sendPlayerMessage(player,"&cThis belongs to someone else.");
                    }
                    //stop event method
                    return;
                }

            }//loop ending
        }
        //check if interaction is inside market region
        if(min != null && max != null ){
            //check if interaction is happening inside market
            if(RegionUtils.isLocationInsideRegion(e.getBlock().getLocation(),min,max) && !Bypass.bypassPlayers.containsKey(player.getUniqueId())){
                e.setCancelled(true);
                SendMessage.sendPlayerMessage(player,"&cThis is not your property.");
                return;
            }
        }

    }
}
