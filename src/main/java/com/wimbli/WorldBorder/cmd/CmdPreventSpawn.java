package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

public class CmdPreventSpawn extends WBCmd
{

    public CmdPreventSpawn() {
        name = permission = "preventmobspawn";
        minParams = maxParams = 1;

        addCmdExample(nameEmphasized() + "<on|off> - stop mob spawning past border.");
        helpText = "Default value: off. When enabled, this setting will prevent mobs from naturally spawning outside the world's border.";
    }

    @Override
    public void cmdStatus(ICommandSender sender)
    {
        Util.chat(sender, C_HEAD + "Prevention of mob spawning outside the border is " + enabledColored(Config.preventMobSpawn()) + C_HEAD + ".");
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        Config.setPreventMobSpawn(strAsBool(params.get(0)));

        if (player != null)
        {
            Log.info((Config.preventMobSpawn() ? "Enabled" : "Disabled") + " preventmobspawn at the command of player \"" + player.getDisplayName() + "\".");
            cmdStatus(sender);
        }
    }
}
