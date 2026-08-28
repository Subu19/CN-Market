package net.craftnepal.market.Listeners;

import me.kodysimpson.simpapi.menu.MenuManager;
import me.kodysimpson.simpapi.menu.PlayerMenuUtility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Field;
import java.util.HashMap;

public class MenuCleanupListener implements Listener {

    private static Field utilityMapField;

    static {
        try {
            utilityMapField = MenuManager.class.getDeclaredField("playerMenuUtilityMap");
            utilityMapField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (utilityMapField == null)
            return;

        try {
            @SuppressWarnings("unchecked")
            HashMap<Player, PlayerMenuUtility> map =
                    (HashMap<Player, PlayerMenuUtility>) utilityMapField.get(null);
            if (map != null) {
                // Remove the quitting player's utility from the map.
                // This ensures that when they rejoin, a new PlayerMenuUtility
                // is created with the new Player instance.
                map.remove(event.getPlayer());
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
