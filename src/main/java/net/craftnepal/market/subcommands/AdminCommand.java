package net.craftnepal.market.subcommands;

import net.craftnepal.market.subcommands.admin.*;

import java.util.List;

public class AdminCommand extends NestedCommand {

    public AdminCommand() {
        registerSubCommand(new Bypass());
        registerSubCommand(new DeletePlot());
        registerSubCommand(new ListPlots());
        registerSubCommand(new Reload());
        registerSubCommand(new SelectionMode());
        registerSubCommand(new SetOwner());
        registerSubCommand(new SetSpawn());
        registerSubCommand(new ToggleBorder());
        registerSubCommand(new Setup());
        registerSubCommand(new ForceUpdate());
        registerSubCommand(new CalcPrices());
        registerSubCommand(new AdminMode());
        registerSubCommand(new Reset());
    }


    @Override
    public String getRequiredPermission() {
        return "market.admin";
    }

    @Override
    public String getName() {
        return "admin";
    }

    @Override
    public String getDescription() {
        return "Administrative commands for the market.";
    }

    @Override
    public String getSyntax() {
        return "/market admin <subcommand>";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }
}
