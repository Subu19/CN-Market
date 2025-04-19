package net.craftnepal.market.utils;

import me.kodysimpson.simpapi.colors.ColorTranslator;
import net.craftnepal.market.Market;
import org.bukkit.entity.Player;

public class SendMessage {
    public SendMessage(Player player, String message){
        player.sendMessage(ColorTranslator.translateColorCodes(Market.getMainConfig().getString("prefix")+message));
    }
    public static void sendPlayerMessage(Player player,String message){
        player.sendMessage(ColorTranslator.translateColorCodes(Market.getMainConfig().getString("prefix")+message));

    }
}
