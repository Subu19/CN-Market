package net.craftnepal.market.utils;

import me.kodysimpson.simpapi.colors.ColorTranslator;
import net.craftnepal.market.Market;
import org.bukkit.entity.Player;

public class SendMessage {

    public static void sendPlayerMessage(Player player,String message){
        player.sendMessage(ColorTranslator.translateColorCodes(Market.settings().prefix() + message));

    }

    public static void sendPlayerComponent(Player player, net.md_5.bungee.api.chat.BaseComponent component) {
        String prefix = Market.settings().prefix();

        net.md_5.bungee.api.chat.TextComponent finalComponent = new net.md_5.bungee.api.chat.TextComponent(
                ColorTranslator.translateColorCodes(prefix)
        );
        finalComponent.addExtra(component);
        player.spigot().sendMessage(finalComponent);
    }
}
