package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

public class CmdDynmapmsg extends WBCmd
{
    public CmdDynmapmsg()
    {
        name = permission = "dynmapmsg";
        minParams = 1;

        addCmdExample(nameEmphasized() + "<text> - DynMap border labels will show this.");
        helpText = "Default value: \"The border of the world.\". If you are running the DynMap plugin and the " +
            commandEmphasized("dynmap") + C_DESC + "command setting is enabled, the borders shown in DynMap will " +
            "be labelled with this text.";
    }

    @Override
    public void cmdStatus(ICommandSender sender)
    {
        Util.chat(sender, C_HEAD + "DynMap border label is set to: " + C_ERR + Config.getDynmapMessage());
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        StringBuilder message = new StringBuilder();
        boolean first = true;
        for (String param : params)
        {
            if (!first)
                message.append(" ");
            message.append(param);
            first = false;
        }

        Config.setDynmapMessage(message.toString());

        if (player != null)
            cmdStatus(sender);
    }
}
