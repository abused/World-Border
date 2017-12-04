package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;


public class CmdRemount extends WBCmd
{
    public CmdRemount()
    {
        name = permission = "remount";
        minParams = maxParams = 1;

        addCmdExample(nameEmphasized() + "<on|off> - turn remount after knockback on or off.");
        helpText = "Default value: on. Players who are knocked back from a border are ejected from their vehicle " +
            "by vanilla design. With this enabled, they will be remounted on their vehicle.";
    }

    @Override
    public void cmdStatus(ICommandSender sender)
    {
        Util.chat(sender, C_HEAD + "Remount after knockback is " + enabledColored(Config.getRemount()) + C_HEAD + ".");
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        Config.setRemount(strAsBool(params.get(0)));

        if (player != null)
        {
            cmdStatus(sender);
            Log.info(
                (Config.getRemount() ? "Enabled" : "Disabled")
                + " remount after knockback at the command of player \""
                + player.getDisplayName() + "\"."
            );
        }
    }
}
