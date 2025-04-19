package net.craftnepal.market.subcommands;

import jdk.vm.ci.code.site.Mark;
import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Market;
import net.craftnepal.market.files.RegionData;
import net.craftnepal.market.utils.SendMessage;
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
        return "Reload market plugin.";
    }

    @Override
    public String getSyntax() {
        return "/amarket reload";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        RegionData.reload();
        Market.reloadMainConfig();
        if(commandSender instanceof Player){
            SendMessage.sendPlayerMessage((Player) commandSender,"Reloaded Market configuration!");
        }else{
            Bukkit.getLogger().info("Reloaded Market configuration!");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
