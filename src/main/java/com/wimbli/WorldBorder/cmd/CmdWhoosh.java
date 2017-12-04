package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;


public class CmdWhoosh extends WBCmd
{
    public CmdWhoosh()
    {
        name = permission = "whoosh";
        minParams = maxParams = 1;

        addCmdExample(nameEmphasized() + "<on|off> - turn knockback effect on or off.");
        helpText = "Default value: on. This will show a particle effect and play a sound where a player is knocked " +
            "back from the border.";
    }

    @Override
    public void cmdStatus(ICommandSender sender)
    {
        Util.chat(sender, C_HEAD + "\"Whoosh\" knockback effect is " + enabledColored(Config.doWhooshEffect()) + C_HEAD + ".");
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        Config.setWhooshEffect(strAsBool(params.get(0)));

        if (player != null)
        {
            Log.info((Config.doWhooshEffect() ? "Enabled" : "Disabled") + " \"whoosh\" knockback effect at the command of player \"" + player.getDisplayName() + "\".");
            cmdStatus(sender);
        }
    }
}
