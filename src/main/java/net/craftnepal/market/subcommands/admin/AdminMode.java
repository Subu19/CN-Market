package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.managers.AdminModeManager;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AdminMode extends SubCommand {
    @Override
    public String getName() {
        return "adminmode";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Toggle Admin Mode for creating infinite admin shops via the normal GUI.";
    }

    @Override
    public String getSyntax() {
        return "/market admin adminmode";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof Player)) return;
        Player player = (Player) commandSender;

        AdminModeManager.toggleAdminMode(player.getUniqueId());
        boolean enabled = AdminModeManager.isInAdminMode(player.getUniqueId());

        SendMessage.sendPlayerMessage(player, "&aAdmin Mode " + (enabled ? "&2ENABLED" : "&cDISABLED") + "&a.");
        if (enabled) {
            SendMessage.sendPlayerMessage(player, "&7Shops you create now will be infinite Admin Shops.");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
