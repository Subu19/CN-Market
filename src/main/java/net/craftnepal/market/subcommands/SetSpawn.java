package net.craftnepal.market.subcommands;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.files.RegionData;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetSpawn extends SubCommand {
    @Override
    public String getName() {
        return "setspawn";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Sets spawn of market.";
    }

    @Override
    public String getSyntax() {
        return "/amarket setspawn";
    }

    @Override
    public void perform(CommandSender commandSender, String[] strings) {
        if(commandSender instanceof Player){
            Player p = (Player) commandSender;
            Location location = p.getLocation();
            RegionData.get().set("market.spawn", location);
            RegionData.save();
            p.sendMessage("Spawn set!");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] strings) {
        return null;
    }
}
