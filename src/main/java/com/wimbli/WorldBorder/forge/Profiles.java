package com.wimbli.WorldBorder.forge;

import com.mojang.authlib.GameProfile;
import com.wimbli.WorldBorder.WorldBorder;
import net.minecraft.server.management.PlayerProfileCache;

import java.util.UUID;

/** Static utility class for player profile shortcuts */
public class Profiles
{
    public static String[] fetchNames(UUID[] uuids)
    {
        PlayerProfileCache cache = WorldBorder.SERVER.getPlayerProfileCache();
        String[]           names = new String[uuids.length];

        // Makes sure server reads from cache first
        cache.load();

        for (int i = 0; i < uuids.length; i++)
        {
            GameProfile profile = cache.getProfileByUUID(uuids[i]);

            names[i] = (profile != null)
                ? profile.getName()
                : "<unknown:" + uuids[i].toString() + ">";
        }

        return names;
    }

    public static UUID fetchUUID(String name)
    {
        GameProfile profile = WorldBorder.SERVER
            .getPlayerProfileCache()
            .getGameProfileForUsername(name);

        if (profile == null)
            throw new RuntimeException(name + " is not a valid user");
        else
            return profile.getId();
    }
}
