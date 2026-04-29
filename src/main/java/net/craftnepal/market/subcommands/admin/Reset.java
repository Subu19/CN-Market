package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.LocationData;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.DisplayUtils;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

public class Reset extends SubCommand {
    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Reset the entire market system (DELETE WORLD & DATA).";
    }

    @Override
    public String getSyntax() {
        return "/amarket reset confirm";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        if (!player.hasPermission("market.admin")) {
            SendMessage.sendPlayerMessage(player, "§cYou don't have permission to use this command.");
            return;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            SendMessage.sendPlayerMessage(player, "§c§lWARNING: This will delete the market world and ALL plot/shop data!");
            SendMessage.sendPlayerMessage(player, "§cTo proceed, type: §f/amarket reset confirm");
            return;
        }

        SendMessage.sendPlayerMessage(player, "§eResetting market system...");

        // 1. Clear displays
        DisplayUtils.getInstance().clearAllDisplays();

        // 2. Unload world
        String worldName = Market.getMainConfig().getString("market-world.name", "market");
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            // Teleport players out
            World overworld = Bukkit.getWorlds().get(0);
            for (Player p : world.getPlayers()) {
                p.teleport(overworld.getSpawnLocation());
                SendMessage.sendPlayerMessage(p, "§cThe market world is being reset. You have been teleported to spawn.");
            }
            
            Bukkit.unloadWorld(world, false);
            SendMessage.sendPlayerMessage(player, "§7Unloaded world '" + worldName + "'.");
        }

        // 3. Delete world folder
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            deleteDirectory(worldFolder);
            SendMessage.sendPlayerMessage(player, "§7Deleted world folder.");
        }

        // 4. Clear Data Files
        RegionData.get().set("market", null);
        RegionData.save();
        
        LocationData.get().set("market", null);
        LocationData.save();
        
        SendMessage.sendPlayerMessage(player, "§7Cleared all region and location data.");

        // 5. Update config
        Market.getMainConfig().set("market-world.name", null);
        Market.getPlugin().saveConfig();
        
        SendMessage.sendPlayerMessage(player, "§a§lMarket system reset successfully!");
        SendMessage.sendPlayerMessage(player, "§eUse §f/market admin setup §eto start over.");
    }

    private void deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            path.delete();
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            return List.of("confirm");
        }
        return null;
    }

    @Override
    public List<String> getAliases() {
        return null;
    }
}
