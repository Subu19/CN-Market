package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.LocationData;
import net.craftnepal.market.files.PriceData;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.managers.DynamicPriceManager;
import net.craftnepal.market.managers.MarketWorldManager;
import net.craftnepal.market.utils.DisplayUtils;
import net.craftnepal.market.utils.SendMessage;
import net.craftnepal.market.world.SchematicManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class Reload extends SubCommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Reload all market plugin data files.";
    }

    @Override
    public String getSyntax() {
        return "/market admin reload";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("market.admin")) {
            SendMessage.sendPlayerMessage((Player) sender, "§cYou don't have permission to use this command.");
            return;
        }

        // 1. config.yml — plugin settings (prefix, world name, flight, etc.)
        Market.reloadMainConfig();

        // 2. price.yml — admin-editable base item prices
        PriceData.reload();

        // 3. RegionData.yml — plot boundaries, owners, shop data
        RegionData.reload();

        // 4. LocationData.yml — saved last-locations for /market back
        LocationData.reload();

        // 5. dynamic_prices.yml + market_metrics.yml — market economy state
        DynamicPriceManager.reload();

        // 6. Schematic manager picks up any config changes (plot/pathway sizes)
        SchematicManager.getInstance().init();

        // 7. Re-apply world spawn from the (potentially updated) RegionData
        MarketWorldManager.initialize(false);

        // 8. Refresh all shop displays so updated prices are visible immediately
        Bukkit.getScheduler().runTask(Market.getPlugin(), () ->
                DisplayUtils.getInstance().updateAllDisplays());

        String msg = "§aMarket reloaded successfully! (config, price, region, location, dynamic prices, schematics, displays)";
        if (sender instanceof Player) {
            SendMessage.sendPlayerMessage((Player) sender, msg);
        } else {
            Bukkit.getLogger().info(msg);
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
