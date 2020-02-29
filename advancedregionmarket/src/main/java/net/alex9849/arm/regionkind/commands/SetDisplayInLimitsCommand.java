package net.alex9849.arm.regionkind.commands;

import net.alex9849.arm.Permission;
import net.alex9849.arm.regionkind.RegionKind;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetDisplayInLimitsCommand extends RegionKindOptionModifyCommand<Boolean> {


    public SetDisplayInLimitsCommand() {
        super("setdisplayinlimits",
                Arrays.asList(Permission.REGIONKIND_SET_DISPLAY_IN_LIMITS), "(false|true)",
                "[true/false]", "");
    }

    @Override
    protected Boolean getSettingsFromString(CommandSender sender, String setting) {
        return Boolean.parseBoolean(setting);
    }

    @Override
    protected void applySetting(CommandSender sender, RegionKind object, Boolean setting) {
        object.setDisplayInLimits(setting);
    }

    @Override
    protected List<String> tabCompleteSettingsObject(Player player, String[] args) {
        if(args.length != 3) {
            return new ArrayList<>();
        }
        List<String> returnme = new ArrayList<>();
        if ("true".startsWith(args[2])) {
            returnme.add("true");
        }
        if ("false".startsWith(args[2])) {
            returnme.add("false");
        }
        return returnme;
    }
}
