package net.craftnepal.market.Listeners;

import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.DisplayUtils;
import net.craftnepal.market.utils.SendMessage;
import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class InternalCommandListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();

        // Internal buy command — triggered only from chat clickable buttons
        if (message.startsWith("/market _buy ")) {
            event.setCancelled(true);
            String[] args = message.substring("/market _buy ".length()).split(" ");
            if (args.length < 3) return;
            try {
                ShopUtils.processPurchase(player, args[0], args[1], Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {}
        }

        // Internal remove-shop command — triggered only from chat clickable buttons
        if (message.startsWith("/market _removeshop ")) {
            event.setCancelled(true);
            String[] args = message.substring("/market _removeshop ".length()).split(" ");
            if (args.length < 2) return;
            String plotId = args[0];
            String shopId = args[1];
            String basePath = "market.plots." + plotId + ".shops." + shopId;
            if (!RegionData.get().contains(basePath)) {
                SendMessage.sendPlayerMessage(player, "§cThis shop no longer exists.");
                return;
            }
            String owner = RegionData.get().getString(basePath + ".owner");
            if (player.hasPermission("market.admin") || (owner != null && owner.equals(player.getUniqueId().toString()))) {
                RegionData.get().set(basePath, null);
                RegionData.save();
                SendMessage.sendPlayerMessage(player, "§aShop removed successfully.");
                DisplayUtils.getInstance().removeDisplayPair(plotId, shopId);
            } else {
                SendMessage.sendPlayerMessage(player, "§cYou do not have permission to remove this shop.");
            }
        }
    }
}
