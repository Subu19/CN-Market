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
        if (!(sender instanceof Player))
            return;
        Player player = (Player) sender;

        if (!player.hasPermission("market.admin")) {
            SendMessage.sendPlayerMessage(player,
                    "§cYou don't have permission to use this command.");
            return;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            SendMessage.sendPlayerMessage(player,
                    "§c§lWARNING: This will delete the market world and ALL plot/shop data!");
            SendMessage.sendPlayerMessage(player, "§cTo proceed, type: §f/amarket reset confirm");
            return;
        }

        SendMessage.sendPlayerMessage(player, "§eResetting market system...");

        // 1. Clear displays
        DisplayUtils.getInstance().clearAllDisplays();

        // 2. Unload world
        String worldName = Market.getMainConfig().getString("market-world.name", "market");
        World world = Bukkit.getWorld(worldName);
        
        // Cache the world folder location before it gets unloaded
        final File cachedWorldFolder = (world != null) ? world.getWorldFolder() : null;

        if (world != null) {
            // Teleport players out
            World overworld = Bukkit.getWorlds().get(0);
            List<Player> worldPlayers = world.getPlayers();
            if (!worldPlayers.isEmpty()) {
                SendMessage.sendPlayerMessage(player,
                        "§7Teleporting " + worldPlayers.size() + " players out of market...");
                for (Player p : worldPlayers) {
                    p.teleport(overworld.getSpawnLocation());
                    SendMessage.sendPlayerMessage(p,
                            "§cThe market world is being reset. You have been teleported to spawn.");
                }
            }
        }

        SendMessage.sendPlayerMessage(player, "§eWaiting a moment for world to clear...");

        Bukkit.getScheduler().runTaskLater(Market.getPlugin(), () -> {
            World worldToUnload = Bukkit.getWorld(worldName);
            if (worldToUnload != null) {
                boolean unloaded = Bukkit.unloadWorld(worldToUnload, false);
                if (unloaded) {
                    SendMessage.sendPlayerMessage(player,
                            "§aSuccessfully unloaded world '" + worldName + "'.");
                } else {
                    SendMessage.sendPlayerMessage(player, "§cFailed to unload world '" + worldName
                            + "'. It might be a primary world or in use.");
                    // We shouldn't proceed to delete if it's still loaded
                    return;
                }
            }

            // 3. Delete world folder
            File worldFolder = cachedWorldFolder;
            if (worldFolder == null) {
                // Fallback guessing if world wasn't loaded
                File standardFolder = new File(Bukkit.getWorldContainer(), worldName);
                File mainWorldFolder = Bukkit.getWorlds().get(0).getWorldFolder();
                File paperFolder = new File(mainWorldFolder, "dimensions/minecraft/" + worldName);
                
                if (paperFolder.exists()) {
                    worldFolder = paperFolder;
                } else {
                    worldFolder = standardFolder;
                }
            }

            if (worldFolder.exists()) {
                SendMessage.sendPlayerMessage(player,
                        "§7Deleting world folder: §f" + worldFolder.getPath());
                deleteDirectory(worldFolder);
                if (worldFolder.exists()) {
                    SendMessage.sendPlayerMessage(player,
                            "§cFailed to fully delete world folder. Some files might be locked.");
                } else {
                    SendMessage.sendPlayerMessage(player, "§7Deleted world folder successfully.");
                }
            } else {
                SendMessage.sendPlayerMessage(player,
                        "§7World folder not found at: " + worldFolder.getAbsolutePath());
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
        }, 20L); // 1 second delay
    }

    private void deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (!file.delete()) {
                            Bukkit.getLogger()
                                    .warning("Failed to delete file: " + file.getAbsolutePath());
                        }
                    }
                }
            }
            if (!path.delete()) {
                Bukkit.getLogger().warning("Failed to delete directory: " + path.getAbsolutePath());
            }
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
