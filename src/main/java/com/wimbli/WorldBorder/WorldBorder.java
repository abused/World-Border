package com.wimbli.WorldBorder;

import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.listener.BlockPlaceListener;
import com.wimbli.WorldBorder.listener.EnderPearlListener;
import com.wimbli.WorldBorder.listener.MobSpawnListener;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Main class and mod definition of WorldBorder-Forge. Holds static references to its
 * singleton instance and management objects. Should only ever be created by Forge.
 */
@Mod(
    modid   = WorldBorder.MODID,
    name    = WorldBorder.MODID,
    version = WorldBorder.VERSION,
    serverSideOnly = true,

    acceptableRemoteVersions = "*",
    acceptableSaveVersions   = ""
)
public class WorldBorder
{
    /** Frozen at 1.0.0 to prevent misleading world save error */
    public static final String VERSION = "1.0.0";
    public static final String MODID   = "WorldBorder";

    /** Singleton instance of WorldBorder, created by Forge */
    public static WorldBorder     INSTANCE = null;
    /** Shortcut reference to vanilla server instance */
    public static MinecraftServer SERVER   = null;
    /** Singleton instance of WorldBorder's command handler */
    public static WBCommand       COMMAND  = null;

    private BlockPlaceListener blockPlaceListener = null;
    private MobSpawnListener   mobSpawnListener   = null;
    private EnderPearlListener enderPearlListener = null;

    /**
     * Given WorldBorder's dependency on dedicated server classes and is designed for
     * use in multiplayer environments, we don't load anything on the client
     */
    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void clientPreInit(FMLPreInitializationEvent event)
    {
        Log.error("This mod is intended only for use on servers");
        Log.error("Please consider removing this mod from your installation");
    }

    @Mod.EventHandler
    @SideOnly(Side.SERVER)
    public void serverPreInit(FMLPreInitializationEvent event)
    {
        Config.setupConfigDir(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    @SideOnly(Side.SERVER)
    public void serverStart(FMLServerStartingEvent event)
    {
        if (INSTANCE == null) INSTANCE = this;
        if (SERVER   == null) SERVER   = event.getServer();
        if (COMMAND  == null) COMMAND  = new WBCommand();

        // Load (or create new) config files
        Config.load(false);

        // our one real command, though it does also have aliases "wb" and "worldborder"
        event.registerServerCommand(COMMAND);

        if ( Config.preventBlockPlace() )
            enableBlockPlaceListener(true);

        if ( Config.preventMobSpawn() )
            enableMobSpawnListener(true);

        if ( Config.getDenyEnderpearl() )
            enableEnderPearlListener(true);

        DynMapFeatures.registerListener();
    }

    @Mod.EventHandler
    @SideOnly(Side.SERVER)
    public void serverPostStart(FMLServerStartedEvent event)
    {
        WBCommand.checkRegistrations(SERVER);
    }

    @Mod.EventHandler
    @SideOnly(Side.SERVER)
    public void serverStop(FMLServerStoppingEvent event)
    {
        DynMapFeatures.removeAllBorders();
        Config.storeFillTask();
    }

    // for other plugins to hook into
    // TODO: use IMC for this?
    @SideOnly(Side.SERVER)
    public BorderData getWorldBorder(String worldName)
    {
        return Config.Border(worldName);
    }

    @SideOnly(Side.SERVER)
    public void enableBlockPlaceListener(boolean enable)
    {
        if      (enable)
            MinecraftForge.EVENT_BUS.register(this.blockPlaceListener = new BlockPlaceListener());
        else if (blockPlaceListener != null)
            MinecraftForge.EVENT_BUS.unregister(this.blockPlaceListener);
    }

    @SideOnly(Side.SERVER)
    public void enableMobSpawnListener(boolean enable)
    {
        if      (enable)
            MinecraftForge.EVENT_BUS.register( this.mobSpawnListener = new MobSpawnListener() );
        else if (mobSpawnListener != null)
            MinecraftForge.EVENT_BUS.unregister(this.mobSpawnListener);
    }

    @SideOnly(Side.SERVER)
    public void enableEnderPearlListener(boolean enable)
    {
        if      (enable)
            MinecraftForge.EVENT_BUS.register( this.enderPearlListener = new EnderPearlListener() );
        else if (enderPearlListener != null)
            MinecraftForge.EVENT_BUS.unregister(this.enderPearlListener);
    }
}
