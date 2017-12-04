package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Profiles;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CmdBypasslist extends WBCmd
{
    public CmdBypasslist()
    {
        name = permission = "bypasslist";
        minParams = maxParams = 0;

        addCmdExample(nameEmphasized() + "- list players with border bypass enabled.");
        helpText = "The bypass list will persist between server restarts, and applies to all worlds. Use the " +
            commandEmphasized("bypass") + C_DESC + "command to add or remove players.";
    }

    @Override
    public void execute(final ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        final UUID[] uuids = Config.getPlayerBypassList();
        if (uuids.length == 0)
        {
            Util.chat(sender, "Players with border bypass enabled: <none>");
            return;
        }

        try
        {
            String[] names = Profiles.fetchNames(uuids);
            String   list  = Arrays.toString(names);

            Util.chat(sender, "Players with border bypass enabled: " + list);
        }
        catch (Exception ex)
        {
            sendErrorAndHelp(sender, "Failed to look up names for the UUIDs in the border bypass list: " + ex.getMessage());
        }
    }
}
