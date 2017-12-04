package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

public class CmdDenypearl extends WBCmd
{
    public CmdDenypearl()
    {
        name = permission = "denypearl";
        minParams = maxParams = 1;

        addCmdExample(nameEmphasized() + "<on|off> - stop ender pearls past the border.");
        helpText = "Default value: on. When enabled, this setting will directly cancel attempts to use an ender pearl to " +
            "get past the border rather than just knocking the player back. This should prevent usage of ender " +
            "pearls to glitch into areas otherwise inaccessible at the border edge.";
    }

    @Override
    public void cmdStatus(ICommandSender sender)
    {
        Util.chat(sender,
            C_HEAD + "Direct cancellation of ender pearls thrown past the border is " +
            enabledColored( Config.getDenyEnderpearl() ) + C_HEAD + "."
        );
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        Config.setDenyEnderpearl( strAsBool( params.get(0) ) );

        if (player != null)
        {
            Log.info(
                (Config.getDenyEnderpearl() ? "Enabled" : "Disabled")
                + " direct cancellation of ender pearls thrown past the border at the command of player \""
                + player.getDisplayName() + "\"."
            );
            cmdStatus(sender);
        }
    }
}
