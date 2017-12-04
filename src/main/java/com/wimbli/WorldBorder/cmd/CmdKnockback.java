package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;


public class CmdKnockback extends WBCmd
{
    public CmdKnockback()
    {
        name = permission = "knockback";
        minParams = maxParams = 1;

        addCmdExample(nameEmphasized() + "<distance> - how far to move the player back.");
        helpText = "Default value: 3.0 (blocks). Players who cross the border will be knocked back to this distance inside.";
    }

    @Override
    public void cmdStatus(ICommandSender sender)
    {
        double kb = Config.getKnockBack();
        if (kb < 1)
            Util.chat(sender, C_HEAD + "Knockback is set to 0, disabling border enforcement.");
        else
            Util.chat(sender, C_HEAD + "Knockback is set to " + kb + " blocks inside the border.");
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        float numBlocks = 0.0F;
        try
        {
            numBlocks = Float.parseFloat(params.get(0));
            if (numBlocks < 0.0 || (numBlocks > 0.0 && numBlocks < 1.0))
                throw new NumberFormatException();
        }
        catch(NumberFormatException ex)
        {
            sendErrorAndHelp(sender, "The knockback must be a decimal value of at least 1.0, or it can be 0.");
            return;
        }

        Config.setKnockBack(numBlocks);

        if (player != null)
            cmdStatus(sender);
    }
}
