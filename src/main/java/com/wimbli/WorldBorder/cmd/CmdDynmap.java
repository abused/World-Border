package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

public class CmdDynmap extends WBCmd
{
    public CmdDynmap()
    {
        name = permission = "dynmap";
        minParams = maxParams = 1;

        addCmdExample(nameEmphasized() + "<on|off> - turn DynMap border display on or off.");
        helpText = "Default value: on. If you are running the DynMap plugin and this setting is enabled, all borders will " +
            "be visually shown in DynMap.";
    }

    @Override
    public void cmdStatus(ICommandSender sender)
    {
        Util.chat(sender, C_HEAD + "DynMap border display is " + enabledColored(Config.isDynmapBorderEnabled()) + C_HEAD + ".");
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        Config.setDynmapBorderEnabled(strAsBool(params.get(0)));

        if (player != null)
        {
            cmdStatus(sender);
            Log.info(
                (Config.isDynmapBorderEnabled() ? "Enabled" : "Disabled")
                + " DynMap border display at the command of player \""
                + player.getDisplayName() + "\"."
            );
        }
    }
}
