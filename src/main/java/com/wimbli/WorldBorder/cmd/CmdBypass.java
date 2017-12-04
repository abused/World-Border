package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.WorldBorder;
import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Profiles;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;
import java.util.UUID;

public class CmdBypass extends WBCmd
{
    public CmdBypass()
    {
        name = permission = "bypass";
        minParams = 0;
        maxParams = 2;

        addCmdExample(nameEmphasized() + "{player} [on|off] - let player go beyond border.");
        helpText = "If [player] isn't specified, command sender is used. If [on|off] isn't specified, the value will " +
            "be toggled. Once bypass is enabled, the player will not be stopped by any borders until bypass is " +
            "disabled for them again. Use the " + commandEmphasized("bypasslist") + C_DESC + "command to list all " +
            "players with bypass enabled.";
    }

    @Override
    public void cmdStatus(ICommandSender sender)
    {
        if (!(sender instanceof EntityPlayerMP))
            return;

        boolean bypass = Config.isPlayerBypassing(((EntityPlayerMP)sender).getUniqueID());
        Util.chat(sender, C_HEAD + "Border bypass is currently " + enabledColored(bypass) + C_HEAD + " for you.");
    }

    @Override
    public void execute(final ICommandSender sender, final EntityPlayerMP player, final List<String> params, String worldName)
    {
        if (player == null && params.isEmpty())
        {
            sendErrorAndHelp(sender, "When running this command from console, you must specify a player.");
            return;
        }

        UUID   uPlayer;
        String sPlayer = params.isEmpty()
            ? player.getDisplayName().toString()
            : params.get(0);

        try
        {
            uPlayer = Profiles.fetchUUID(sPlayer);
        }
        catch (Exception ex)
        {
            sendErrorAndHelp(
                sender,
                "Failed to look up UUID for the player name you specified: " + ex.getLocalizedMessage()
            );
            return;
        }

        boolean bypassing = !Config.isPlayerBypassing(uPlayer);
        if (params.size() > 1)
            bypassing = strAsBool(params.get(1));

        Config.setPlayerBypass(uPlayer, bypassing);

        // If target is online, notify them
        EntityPlayerMP target = WorldBorder.SERVER.getPlayerList().getPlayerByUsername(sPlayer);
        if (target != null)
            Util.chat(target, "Border bypass is now " + enabledColored(bypassing) + " for you.");

        if (player != target)
            Util.chat(sender, "Border bypass for player \"" + sPlayer + "\" is " + enabledColored(bypassing) + ".");

        Log.info(
            "Border bypass for player \"" + sPlayer + "\" is "
            + (bypassing ? "enabled" : "disabled")
            + (player == null
                ? "."
                : " at the command of \"" + player.getDisplayName() + "\".")
        );
    }
}
