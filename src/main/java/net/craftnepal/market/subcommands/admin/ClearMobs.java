package net.craftnepal.market.subcommands.admin;

import me.kodysimpson.simpapi.command.SubCommand;
import net.craftnepal.market.Market;
import net.craftnepal.market.protection.EnvironmentListener;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.List;

/** One-off removal of mobs in the market world. Never touches named, tamed, leashed or ridden mobs, or villagers. */
public class ClearMobs extends SubCommand {

    @Override
    public String getName() { return "clearmobs"; }

    @Override
    public List<String> getAliases() { return null; }

    @Override
    public String getDescription() { return "Remove unowned mobs from the market world"; }

    @Override
    public String getSyntax() { return "/market admin clearmobs <hostile|animals|all>"; }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!sender.hasPermission("market.admin")) {
            SendMessage.sendPlayerMessage(sender, "§cYou don't have permission.");
            return;
        }
        String mode = args.length > 1 ? args[1].toLowerCase() : "";
        if (!List.of("hostile", "animals", "all").contains(mode)) {
            SendMessage.sendPlayerMessage(sender, "§cUsage: " + getSyntax());
            return;
        }
        World world = Market.getPlugin().getMarketWorld();
        if (world == null) {
            SendMessage.sendPlayerMessage(sender, "§cThe market world is not loaded.");
            return;
        }
        int removed = 0;
        for (Mob mob : world.getEntitiesByClass(Mob.class)) {
            boolean hostile = mob instanceof Enemy;
            if (mode.equals("hostile") && !hostile || mode.equals("animals") && hostile) continue;
            if (mob instanceof AbstractVillager || !unowned(mob)) continue;
            mob.remove();
            removed++;
        }
        SendMessage.sendPlayerMessage(sender, "§aRemoved " + removed + " " + mode
                + " mobs (loaded chunks only; named, tamed, leashed and ridden mobs and villagers are kept).");
    }

    /** Like {@link EnvironmentListener#isWild} but includes animals, which the game never despawns. */
    private static boolean unowned(Mob mob) {
        return mob.getCustomName() == null && !mob.isLeashed() && mob.getVehicle() == null
                && mob.getPassengers().isEmpty()
                && !(mob instanceof org.bukkit.entity.Tameable t && t.isTamed());
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return args.length == 3 ? List.of("hostile", "animals", "all") : null;
    }
}
