package net.craftnepal.market.subcommands;

import net.craftnepal.market.subcommands.plot.*;

import java.util.List;

public class PlotCommand extends NestedCommand {

    public PlotCommand() {
        registerSubCommand(new Claim());
        registerSubCommand(new PlotSpawn());
        registerSubCommand(new PlotTeleport());
        registerSubCommand(new SetPlotSpawn());
        registerSubCommand(new AddMember());
        registerSubCommand(new RemoveMember());
        registerSubCommand(new Unclaim());
        registerSubCommand(new Info());
    }
    
    @Override
    public String getRequiredPermission() {
        return "market.use";
    }

    @Override
    public String getName() {
        return "plot";
    }

    @Override
    public String getDescription() {
        return "Manage your market plots.";
    }

    @Override
    public String getSyntax() {
        return "/market plot <subcommand>";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }
}
