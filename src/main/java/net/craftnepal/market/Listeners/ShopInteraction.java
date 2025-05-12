package net.craftnepal.market.Listeners;

import net.craftnepal.market.Market;
import net.craftnepal.market.files.PriceData;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.MarketUtils;
import net.craftnepal.market.utils.PlotUtils;
import net.craftnepal.market.utils.RegionUtils;
import net.craftnepal.market.utils.SendMessage;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class ShopInteraction implements Listener {
    private final Map<UUID, Consumer<String>> awaitingInput = new HashMap<>();

    @EventHandler
    public void onChestInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null) return;

        Block clickedBlock = event.getClickedBlock();
        Material blockType = clickedBlock.getType();

        if (blockType != Material.CHEST && blockType != Material.TRAPPED_CHEST) return;

        Location chestLocation = clickedBlock.getLocation();

        // First check if this is inside market area
        if (!MarketUtils.isInMarketArea(chestLocation)) {
            return; // Do nothing if not in market area
        }

        // Check if chest is inside the player's plot
        String plot = PlotUtils.getPlotIdByLocation(chestLocation);
        String playerPlot;

        if (PlotUtils.getPlotOwner(plot).equals(uuid.toString())) {
            playerPlot = plot;
        } else {
            playerPlot = null;
        }

        if (playerPlot == null) {
            SendMessage.sendPlayerMessage(player, "§cYou must click a chest inside your own plot.");
            event.setCancelled(true);
            return;
        }

        // Check the item in hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            SendMessage.sendPlayerMessage(player, "§cYou must be holding an item to create a shop.");
            event.setCancelled(true);
            return;
        }

        // Check if this chest is already a shop
        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + playerPlot + ".shops");
        if (shops != null) {
            for (String shopId : shops.getKeys(false)) {
                Location shopLoc = RegionData.get().getLocation("market.plots." + playerPlot + ".shops." + shopId + ".location");
                if (shopLoc != null && shopLoc.equals(chestLocation)) {
                    SendMessage.sendPlayerMessage(player, "§cThis chest is already a shop!");
                    event.setCancelled(true);
                    return;
                }
            }
        }        // Retrieve the item in hand
        Material itemType = item.getType();
        String itemName = itemType.name().replace("_", " ").toLowerCase();        // Fetch the fair price from PriceData
        double fairPrice = PriceData.getPrice(itemType);
        if (fairPrice <= 0) {
            SendMessage.sendPlayerMessage(player, "§cThis item cannot be sold in shops as it has no base price set.");
            event.setCancelled(true);
            return;
        }

        double minPrice = fairPrice * 0.85; // 15% below base price
        double maxPrice = fairPrice * 1.50; // 50% above base price
        String priceDisplay = String.format("§e%.2f", fairPrice);

        // Send detailed messages to the player
        SendMessage.sendPlayerMessage(player, "§7=============================");
        SendMessage.sendPlayerMessage(player, "§aYou're creating a shop for: §b" + itemName);
        SendMessage.sendPlayerMessage(player, "§7Base price: " + priceDisplay);
        SendMessage.sendPlayerMessage(player, String.format("§7Price range: §c%.2f §7to §a%.2f", minPrice, maxPrice));
        SendMessage.sendPlayerMessage(player, "§7----------------------------------------");
        SendMessage.sendPlayerMessage(player, "   ");

        // Create interactive buttons in multiple components
        TextComponent spacer = new TextComponent("    ");

        // Create price adjustment buttons
        TextComponent decrease10 = new TextComponent("§6[<< 10%]");
        decrease10.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("%.2f", fairPrice * 0.90)));
        decrease10.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("§7Set price to: §6" + String.format("%.2f", fairPrice * 0.90)).create()));

        TextComponent decrease5 = new TextComponent("§e[< 5%]");
        decrease5.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("%.2f", fairPrice * 0.95)));
        decrease5.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("§7Set price to: §e" + String.format("%.2f", fairPrice * 0.95)).create()));

        TextComponent basePrice = new TextComponent("§2[" + String.format("%.2f", fairPrice) + "]");
        basePrice.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("%.2f", fairPrice)));
        basePrice.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("§7Click to use base price").create()));

        TextComponent increase5 = new TextComponent("§e[5% >]");
        increase5.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("%.2f", fairPrice * 1.05)));
        increase5.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("§7Set price to: §e" + String.format("%.2f", fairPrice * 1.05)).create()));

        TextComponent increase10 = new TextComponent("§6[10% >>]");
        increase10.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("%.2f", fairPrice * 1.10)));
        increase10.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("§7Set price to: §6" + String.format("%.2f", fairPrice * 1.10)).create()));

        // Create the bottom row with all buttons
        TextComponent buttonsRow = new TextComponent(Objects.requireNonNull(Market.getMainConfig().getString("prefix")).replaceAll("&","§"));
        buttonsRow.addExtra(decrease10);
        buttonsRow.addExtra(spacer);
        buttonsRow.addExtra(decrease5);
        buttonsRow.addExtra(spacer);
        buttonsRow.addExtra(basePrice);
        buttonsRow.addExtra(spacer);
        buttonsRow.addExtra(increase5);
        buttonsRow.addExtra(spacer);
        buttonsRow.addExtra(increase10);

        player.spigot().sendMessage(buttonsRow);
        SendMessage.sendPlayerMessage(player, "   ");
        SendMessage.sendPlayerMessage(player, "§7----------------------------------------");
        // Additional instruction
        SendMessage.sendPlayerMessage(player, "§aType a custom price in chat");
        SendMessage.sendPlayerMessage(player, "§7(Type §fcancel §7in chat to exit)");

        awaitingInput.put(uuid, (input) -> {
            if (input.equalsIgnoreCase("cancel")) {
                SendMessage.sendPlayerMessage(player, "§cShop creation cancelled.");
                return;
            }
              try {
                double price = Double.parseDouble(input);
                if (price < minPrice) {
                    player.sendMessage(ChatColor.RED + "Price cannot be less than " + String.format("%.2f", minPrice) + " (15% below base price).");
                    return;
                }
                if (price > maxPrice) {
                    player.sendMessage(ChatColor.RED + "Price cannot be more than " + String.format("%.2f", maxPrice) + " (50% above base price).");
                    return;
                }

                // Generate a new shop ID
                String shopId = UUID.randomUUID().toString();

                // Save shop data to YAML
                RegionData.get().set("market.plots." + playerPlot + ".shops." + shopId + ".location", chestLocation);
                RegionData.get().set("market.plots." + playerPlot + ".shops." + shopId + ".item", item.getType().toString());
                RegionData.get().set("market.plots." + playerPlot + ".shops." + shopId + ".owner", uuid.toString());
                RegionData.get().set("market.plots." + playerPlot + ".shops." + shopId + ".price", price);

                // Save the file
                RegionData.save();

                player.sendMessage(ChatColor.GREEN + "Shop created successfully with price: " + ChatColor.GOLD + price);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid price entered. Please enter a valid number.");
            }        });

        // Add timeout to remove awaitingInput after 30 seconds
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("Market"), () -> {
            if (awaitingInput.remove(uuid) != null) {
                SendMessage.sendPlayerMessage(player, "§cShop creation timed out after 30 seconds.");
            }
        }, 20L * 30); // 30 seconds * 20 ticks per second

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (awaitingInput.containsKey(uuid)) {
            event.setCancelled(true);
            Consumer<String> consumer = awaitingInput.remove(uuid);
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Market"), () -> {
                consumer.accept(event.getMessage());
            });
        }
    }

    @EventHandler
    public void onChestBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material blockType = block.getType();

        if (blockType != Material.CHEST && blockType != Material.TRAPPED_CHEST) return;

        Location chestLocation = block.getLocation();

        // First check if this is inside market area
        if (!MarketUtils.isInMarketArea(chestLocation)) {
            return; // Do nothing if not in market area
        }

        // Check all plots for this chest
        ConfigurationSection plots = RegionData.get().getConfigurationSection("market.plots");
        if (plots != null) {
            for (String plot : plots.getKeys(false)) {
                ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plot + ".shops");
                if (shops != null) {
                    for (String shopId : shops.getKeys(false)) {
                        Location shopLoc = RegionData.get().getLocation("market.plots." + plot + ".shops." + shopId + ".location");
                        if (shopLoc != null && shopLoc.equals(chestLocation)) {
                            // Check if the breaker is the owner or has permission
                            Player player = event.getPlayer();
                            String owner = RegionData.get().getString("market.plots." + plot + ".shops." + shopId + ".owner");

                            if (owner != null && owner.equals(player.getUniqueId().toString())) {
                                // Owner breaking their own shop - remove it
                                RegionData.get().set("market.plots." + plot + ".shops." + shopId, null);
                                RegionData.save();
                                SendMessage.sendPlayerMessage(player,"§aShop removed successfully.");
                            } else {
                                // Someone else trying to break the shop
                                SendMessage.sendPlayerMessage(player,"§cYou cannot break someone else's shop!");
                                event.setCancelled(true);
                            }
                            return;
                        }
                    }
                }
            }
        }
    }
}
