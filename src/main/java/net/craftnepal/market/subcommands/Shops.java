package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import me.kodysimpson.simpapi.exceptions.MenuManagerException;
import me.kodysimpson.simpapi.exceptions.MenuManagerNotSetupException;
import me.kodysimpson.simpapi.menu.MenuManager;
import net.craftnepal.market.menus.ShopListMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

public class Shops extends SubCommand {

    @Override
    public String getName() {
        return "shops";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "GUI for all the shop list.";
    }

    @Override
    public String getSyntax() {
        return "/market shops";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return;
        }

        Player player = (Player) sender;

        try {
            MenuManager.openMenu(ShopListMenu.class, player);
        } catch (MenuManagerException | MenuManagerNotSetupException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}