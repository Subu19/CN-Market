package net.craftnepal.market.Listeners;

import me.kodysimpson.simpapi.menu.MenuManager;
import net.craftnepal.market.Market;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * SimpAPI keeps a menu utility per {@code Player} object and never drops it, so a rejoining
 * player would get a stale one. This removes it on quit through reflection on a private field.
 * If a SimpAPI update renames the field, this logs once and does nothing.
 */
public class MenuCleanupListener implements Listener {

    private static final Field utilityMapField = findField();

    private static Field findField() {
        try {
            Field field = MenuManager.class.getDeclaredField("playerMenuUtilityMap");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            Market.getPlugin().getLogger().warning("SimpAPI changed; menu data is not cleaned up on quit: " + e);
            return null;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (utilityMapField == null) return;
        try {
            if (utilityMapField.get(null) instanceof Map<?, ?> map) map.remove(event.getPlayer());
        } catch (IllegalAccessException e) {
            // setAccessible succeeded above; cannot happen in practice.
        }
    }
}
