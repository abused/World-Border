package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

public class CmdPreventPlace extends WBCmd
{

    public CmdPreventPlace() {
        name = permission = "preventblockplace";
        minParams = maxParams = 1;

        addCmdExample(nameEmphasized() + "<on|off> - stop block placement past border.");
        helpText = "Default value: off. When enabled, this setting will prevent players from placing blocks outside the world's border.";
    }

    @Override
    public void cmdStatus(ICommandSender sender)
    {
        Util.chat(sender, C_HEAD + "Prevention of block placement outside the border is " + enabledColored(Config.preventBlockPlace()) + C_HEAD + ".");
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        Config.setPreventBlockPlace(strAsBool(params.get(0)));

        if (player != null)
        {
            Log.info((Config.preventBlockPlace() ? "Enabled" : "Disabled") + " preventblockplace at the command of player \"" + player.getDisplayName() + "\".");
            cmdStatus(sender);
        }
    }
}
