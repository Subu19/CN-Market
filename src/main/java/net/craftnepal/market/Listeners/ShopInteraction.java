package net.craftnepal.market.Listeners;

import net.craftnepal.market.Entities.ChestShop;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.PriceData;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.*;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.block.Barrel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class ShopInteraction implements Listener {
    private final Map<UUID, Consumer<String>> awaitingInput = new HashMap<>();
    private final DisplayUtils displayUtils;

    public ShopInteraction() {
        this.displayUtils = DisplayUtils.getInstance();
    }

    @EventHandler
    public void onChestInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check if the player is not in survival mode.
        if (player.getGameMode() != GameMode.SURVIVAL)
            return;

        // Check for block and click
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null)
            return;

        Block clickedBlock = event.getClickedBlock();
        Material blockType = clickedBlock.getType();

        if (blockType != Material.BARREL) {
            return;
        }

        Location chestLocation = clickedBlock.getLocation();

        // First check if this is inside market area
        if (!MarketUtils.isInMarketArea(chestLocation)) {
            return; // Do nothing if not in market area
        }

        // Check if chest is inside a plot
        String plot = PlotUtils.getPlotIdByLocation(chestLocation);
        if (plot == null) {
            SendMessage.sendPlayerMessage(player, "§cThis barrel is not inside a market plot.");
            event.setCancelled(true);
            return;
        }

        // Members can also see their own shop stats (treated as co-owners)
        String owner = PlotUtils.getPlotOwner(plot);
        if (owner == null || !owner.equals(uuid.toString())) {
            // check members
            java.util.List<String> members = RegionData.get().getStringList("market.plots." + plot + ".members");
            if (!player.hasPermission("market.admin") && !members.contains(uuid.toString())) {
                SendMessage.sendPlayerMessage(player, "§cYou can only create shops in your own plot.");
                event.setCancelled(true);
                return;
            }
        }

        String playerPlot = plot;

        // Check if this chest is already a shop
        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + playerPlot + ".shops");
        if (shops != null) {
            for (String shopId : shops.getKeys(false)) {
                Location shopLoc = LocationUtils.loadLocation(RegionData.get(),
                        "market.plots." + playerPlot + ".shops." + shopId + ".location");
                if (shopLoc != null && shopLoc.equals(chestLocation)) {
                    String shopOwner = RegionData.get().getString("market.plots." + playerPlot + ".shops." + shopId + ".owner");
                    java.util.List<String> members = RegionData.get().getStringList("market.plots." + playerPlot + ".members");
                    if (player.hasPermission("market.admin") || (shopOwner != null && shopOwner.equals(player.getUniqueId().toString())) || members.contains(player.getUniqueId().toString())) {
                        showShopStats(player, playerPlot, shopId);
                    } else {
                        SendMessage.sendPlayerMessage(player, "§cThis chest is already a shop!");
                    }
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Check the item in hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            SendMessage.sendPlayerMessage(player, "§cYou must be holding an item to create a shop.");
            event.setCancelled(true);
            return;
        }

        // Retrieve the item in hand
        Material itemType = item.getType();
        String itemName = net.craftnepal.market.utils.ShopUtils.getShopDisplayName(item);

        // Get the base price to check if item is sellable
        String itemKey = net.craftnepal.market.utils.ShopUtils.getItemKey(item);
        Integer basePriceValue = PriceData.getPrice(itemKey);
        if (basePriceValue == null || basePriceValue <= 0) {
            SendMessage.sendPlayerMessage(player, "§cThis item cannot be sold in shops as it has no base price set.");
            event.setCancelled(true);
            return;
        }
        double fairPrice = net.craftnepal.market.managers.DynamicPriceManager.getDynamicPrice(itemKey);

        double minPrice = fairPrice * 0.85; // 15% below base price
        double maxPrice = fairPrice * 1.50; // 50% above base price
        String priceDisplay = String.format("§e%.2f", fairPrice);

        // Send detailed messages to the player
        String trendStr = net.craftnepal.market.managers.DynamicPriceManager.getTrendString(itemKey);
        SendMessage.sendPlayerMessage(player, "§7=============================");
        
        net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent("§aYou're creating a shop for: ");
        net.md_5.bungee.api.chat.TextComponent itemCmp = new net.md_5.bungee.api.chat.TextComponent("§b[" + itemName + "]");

        StringBuilder hoverText = new StringBuilder();
        hoverText.append("§e").append(itemName);
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            for (String line : item.getItemMeta().getLore()) {
                hoverText.append("\n").append(line);
            }
        }
        if (item.hasItemMeta() && item.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
            org.bukkit.inventory.meta.EnchantmentStorageMeta meta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) item.getItemMeta();
            for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : meta.getStoredEnchants().entrySet()) {
                hoverText.append("\n§7").append(net.craftnepal.market.utils.ShopUtils.formatKey(entry.getKey().getKey().getKey())).append(" ").append(entry.getValue());
            }
        }
        itemCmp.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder(hoverText.toString()).create()
        ));
        msg.addExtra(itemCmp);
        player.spigot().sendMessage(msg);
        SendMessage.sendPlayerMessage(player, "§7Market price: " + priceDisplay + " " + trendStr);
        SendMessage.sendPlayerMessage(player, String.format("§7Price range: §c%.2f §7to §a%.2f", minPrice, maxPrice));
        SendMessage.sendPlayerMessage(player, "§7----------------------------------------");
        SendMessage.sendPlayerMessage(player, "   ");

        // Create interactive buttons in multiple components
        TextComponent spacer = new TextComponent("    ");

        // Create price adjustment buttons
        TextComponent decrease10 = new TextComponent("§6[<< 10%]");
        decrease10.setClickEvent(
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("%.2f", fairPrice * 0.90)));
        decrease10.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Set price to: §6" + String.format("%.2f", fairPrice * 0.90)).create()));

        TextComponent decrease5 = new TextComponent("§e[< 5%]");
        decrease5.setClickEvent(
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("%.2f", fairPrice * 0.95)));
        decrease5.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Set price to: §e" + String.format("%.2f", fairPrice * 0.95)).create()));

        TextComponent basePrice = new TextComponent("§2[" + String.format("%.2f", fairPrice) + "]");
        basePrice.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("%.2f", fairPrice)));
        basePrice.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Click to use base price").create()));

        TextComponent increase5 = new TextComponent("§e[5% >]");
        increase5.setClickEvent(
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("%.2f", fairPrice * 1.05)));
        increase5.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Set price to: §e" + String.format("%.2f", fairPrice * 1.05)).create()));

        TextComponent increase10 = new TextComponent("§6[10% >>]");
        increase10.setClickEvent(
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.format("%.2f", fairPrice * 1.10)));
        increase10.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Set price to: §6" + String.format("%.2f", fairPrice * 1.10)).create()));

        // Create the bottom row with all buttons
        TextComponent buttonsRow = new TextComponent(
                Objects.requireNonNull(Market.getMainConfig().getString("prefix")).replaceAll("&", "§"));
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
                // Parse the input price
                double price = Double.parseDouble(input);

                // Validate the price
                if (price < minPrice) {
                    player.sendMessage(ChatColor.RED + "Price cannot be less than " + String.format("%.2f", minPrice)
                            + " (15% below base price).");
                    return;
                }
                if (price > maxPrice) {
                    player.sendMessage(ChatColor.RED + "Price cannot be more than " + String.format("%.2f", maxPrice)
                            + " (50% above base price).");
                    return;
                }

                // Generate a new shop ID
                String shopId = UUID.randomUUID().toString();

                ItemStack shopItem = item.clone();
                shopItem.setAmount(1);

                // Create a new ChestShop instance
                ChestShop chestShop = new ChestShop(
                        shopId,
                        chestLocation,
                        shopItem,
                        uuid,
                        price);

                // Define the base path in the YAML configuration
                String basePath = "market.plots." + playerPlot + ".shops." + shopId;

                // Serialize and save the ChestShop data to the YAML configuration
                LocationUtils.saveLocation(RegionData.get(), basePath + ".location", chestShop.getLocation());
                String base64Item = java.util.Base64.getEncoder().encodeToString(net.craftnepal.market.utils.ShopUtils.serializeItem(shopItem));
                RegionData.get().set(basePath + ".item_bytes", base64Item);
                RegionData.get().set(basePath + ".item", chestShop.getItem().getType().toString()); // Fallback / reference
                RegionData.get().set(basePath + ".owner", chestShop.getOwner().toString());
                RegionData.get().set(basePath + ".price", chestShop.getPrice());

                // Save the configuration to persist the data
                RegionData.save();

                // Notify the player
                player.sendMessage(
                        ChatColor.GREEN + "Shop created successfully with price: " + ChatColor.GOLD + price);

                // Spawn display after creation
                displayUtils.spawnDisplayPair(chestShop);

            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid price entered. Please enter a valid number.");
            }
        });

        // Add timeout to remove awaitingInput after 30 seconds
        Bukkit.getScheduler().runTaskLater(Market.getPlugin(), () -> {
            if (awaitingInput.remove(uuid) != null) {
                SendMessage.sendPlayerMessage(player, "§cShop creation timed out after 30 seconds.");
            }
        }, 20L * 30); // 30 seconds * 20 ticks per second

    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (awaitingInput.containsKey(uuid)) {
            event.setCancelled(true);
            Consumer<String> consumer = awaitingInput.remove(uuid);
            Bukkit.getScheduler().runTask(Market.getPlugin(), () -> {
                consumer.accept(event.getMessage());
            });
        }
    }

    @EventHandler
    public void onVisitorShopClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null)
            return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock.getType() != Material.BARREL)
            return;

        Location chestLocation = clickedBlock.getLocation();

        if (!MarketUtils.isInMarketArea(chestLocation)) {
            return;
        }

        String plot = PlotUtils.getPlotIdByLocation(chestLocation);
        if (plot == null) return;

        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plot + ".shops");
        if (shops == null) return;

        for (String shopId : shops.getKeys(false)) {
            Location shopLoc = LocationUtils.loadLocation(RegionData.get(), "market.plots." + plot + ".shops." + shopId + ".location");
            if (shopLoc != null && shopLoc.equals(chestLocation)) {
                String owner = RegionData.get().getString("market.plots." + plot + ".shops." + shopId + ".owner");
                
                // Owner and members can open the barrel freely
                java.util.List<String> members = RegionData.get().getStringList("market.plots." + plot + ".members");
                if (owner != null && (owner.equals(player.getUniqueId().toString()) || members.contains(player.getUniqueId().toString()))) {
                    return;
                }

                // It's a visitor! Cancel opening the barrel.
                event.setCancelled(true);

                // Fetch shop data
                String itemNameStr = RegionData.get().getString("market.plots." + plot + ".shops." + shopId + ".item");
                Material itemType = Material.matchMaterial(itemNameStr);
                if (itemType == null) return;
                
                double price = RegionData.get().getDouble("market.plots." + plot + ".shops." + shopId + ".price");
                
                // Count stock
                org.bukkit.block.Barrel barrel = (org.bukkit.block.Barrel) clickedBlock.getState();
                int stock = 0;
                for (ItemStack item : barrel.getInventory().getContents()) {
                    if (item != null && item.getType() == itemType) {
                        stock += item.getAmount();
                    }
                }

                // Build Interactive Chat Menu
                ChestShop shop = net.craftnepal.market.utils.ShopUtils.getShop(plot, shopId);
                String itemKey = net.craftnepal.market.utils.ShopUtils.getItemKey(shop);
                String itemName = net.craftnepal.market.utils.ShopUtils.getShopDisplayName(shop);
                String trendStr = net.craftnepal.market.managers.DynamicPriceManager.getTrendString(itemKey);
                SendMessage.sendPlayerMessage(player, "§7=============================");
                SendMessage.sendPlayerMessage(player, "§aShop Item: §b" + itemName);
                SendMessage.sendPlayerMessage(player, "§7Price per item: §e" + EconomyUtils.format(price) + " " + trendStr);
                SendMessage.sendPlayerMessage(player, "§7In Stock: §a" + stock);
                SendMessage.sendPlayerMessage(player, "§7----------------------------------------");
                
                TextComponent spacer = new TextComponent("   ");
                TextComponent buttonsRow = new TextComponent(
                        Objects.requireNonNull(Market.getMainConfig().getString("prefix")).replaceAll("&", "§"));
                
                int[] buyAmounts = {1, 16, 64};
                for (int amount : buyAmounts) {
                    String color = (stock >= amount) ? "§a" : "§c";
                    TextComponent buyBtn = new TextComponent(color + "[Buy " + amount + "]");
                    buyBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/market _buy " + plot + " " + shopId + " " + amount));
                    buyBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder("§7Buy " + amount + " for §e" + EconomyUtils.format(price * amount) + 
                                    (stock < amount ? " §c(Not enough stock)" : "")).create()));
                    buttonsRow.addExtra(buyBtn);
                    buttonsRow.addExtra(spacer);
                }

                player.spigot().sendMessage(buttonsRow);
                SendMessage.sendPlayerMessage(player, "§7=============================");

                return;
            }
        }
    }

    private void showShopStats(Player player, String plotId, String shopId) {
        ConfigurationSection shopSection = RegionData.get().getConfigurationSection("market.plots." + plotId + ".shops." + shopId);
        if (shopSection == null) return;

        String itemNameStr = shopSection.getString("item");
        Material itemType = Material.matchMaterial(itemNameStr);
        if (itemType == null) return;

        double price = shopSection.getDouble("price");
        String ownerUUID = shopSection.getString("owner");
        Location loc = LocationUtils.loadLocation(RegionData.get(), "market.plots." + plotId + ".shops." + shopId + ".location");

        int stock = 0;
        if (loc != null && loc.getBlock().getType() == Material.BARREL) {
            org.bukkit.block.Barrel barrel = (org.bukkit.block.Barrel) loc.getBlock().getState();
            for (ItemStack item : barrel.getInventory().getContents()) {
                if (item != null && item.getType() == itemType) {
                    stock += item.getAmount();
                }
            }
        }

        ChestShop shop = net.craftnepal.market.utils.ShopUtils.getShop(plotId, shopId);
        String itemKey = net.craftnepal.market.utils.ShopUtils.getItemKey(shop);
        String itemName = net.craftnepal.market.utils.ShopUtils.getShopDisplayName(shop);
        String trendStr = net.craftnepal.market.managers.DynamicPriceManager.getTrendString(itemKey);
        SendMessage.sendPlayerMessage(player, "§7=============================");
        SendMessage.sendPlayerMessage(player, "§6§lSHOP STATS");
        SendMessage.sendPlayerMessage(player, "§aItem: §b" + itemName);
        SendMessage.sendPlayerMessage(player, "§7Price: §e" + EconomyUtils.format(price) + " " + trendStr);
        SendMessage.sendPlayerMessage(player, "§7Stock: §a" + stock);
        
        if (player.hasPermission("market.admin")) {
            String ownerName = ownerUUID != null ? Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)).getName() : "Unknown";
            SendMessage.sendPlayerMessage(player, "§7Owner: §f" + ownerName);
        }
        
        SendMessage.sendPlayerMessage(player, "§7----------------------------------------");

        TextComponent removeBtn = new TextComponent("§c§l[REMOVE SHOP]");
        removeBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/market _removeshop " + plotId + " " + shopId));
        removeBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Click to remove this shop permanently").create()));

        TextComponent row = new TextComponent(Objects.requireNonNull(Market.getMainConfig().getString("prefix")).replaceAll("&", "§"));
        row.addExtra(removeBtn);

        player.spigot().sendMessage(row);
        SendMessage.sendPlayerMessage(player, "§7=============================");
    }

    @EventHandler
    public void onChestBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material blockType = block.getType();

        if (blockType != Material.BARREL)
            return;

        Location chestLocation = block.getLocation();

        // First check if this is inside market area
        if (!MarketUtils.isInMarketArea(chestLocation)) {
            return; // Do nothing if not in market area
        }

        // Get plot at chest location
        String plot = PlotUtils.getPlotIdByLocation(chestLocation);
        if (plot == null) return;

        ConfigurationSection shops = RegionData.get().getConfigurationSection("market.plots." + plot + ".shops");
        if (shops == null) return;

        for (String shopId : shops.getKeys(false)) {
            Location shopLoc = LocationUtils.loadLocation(RegionData.get(), "market.plots." + plot + ".shops." + shopId + ".location");
            if (shopLoc != null && shopLoc.equals(chestLocation)) {
                Player player = event.getPlayer();
                String owner = RegionData.get().getString("market.plots." + plot + ".shops." + shopId + ".owner");

                if (player.hasPermission("market.admin") || (owner != null && owner.equals(player.getUniqueId().toString()))) {
                    RegionData.get().set("market.plots." + plot + ".shops." + shopId, null);
                    RegionData.save();
                    SendMessage.sendPlayerMessage(player, "§aShop removed successfully.");
                    displayUtils.removeDisplayPair(plot, shopId);
                } else {
                    SendMessage.sendPlayerMessage(player, "§cYou cannot break someone else's shop!");
                    event.setCancelled(true);
                }
                return;
            }
        }
    }

}
