package net.craftnepal.market.Listeners;

import net.craftnepal.market.utils.ShopUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class InternalCommandListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.startsWith("/market _buy ")) {
            event.setCancelled(true);
            
            String[] args = message.substring("/market _buy ".length()).split(" ");
            if (args.length < 3) return;

            String plotId = args[0];
            String shopId = args[1];
            try {
                int amount = Integer.parseInt(args[2]);
                ShopUtils.processPurchase(event.getPlayer(), plotId, shopId, amount);
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
