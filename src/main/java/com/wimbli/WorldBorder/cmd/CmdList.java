package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;
import java.util.Set;


public class CmdList extends WBCmd
{
    public CmdList()
    {
        name = permission = "list";
        minParams = maxParams = 0;

        addCmdExample(nameEmphasized() + "- show border information for all worlds.");
        helpText = "This command will list full information for every border you have set including position, " +
            "radius, and shape. The default border shape will also be indicated.";
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        Util.chat(sender, "Default border shape for all worlds is \"" + Config.getShapeName() + "\".");

        Set<String> list = Config.BorderDescriptions();

        if (list.isEmpty())
        {
            Util.chat(sender, "There are no borders currently set.");
            return;
        }

        for(String borderDesc : list)
            Util.chat(sender, borderDesc);
    }
}
