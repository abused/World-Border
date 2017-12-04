package com.wimbli.WorldBorder;

import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Worlds;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.CircleMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Static class for integrating with the Dynmap API. All methods fail safely and silently
 * if the API is not available.
 */
public class DynMapFeatures
{
    /**
     * This Gateway inner class is used for holding variables that use Dynmap API types,
     * so that a NoClassDefFound exception is not thrown if the API is missing.
     *
     * TODO: This is very hacky; is there a better way?
     */
    private static class Gateway
    {
        private static DynmapCommonAPI api;
        private static MarkerAPI       markApi;
        private static MarkerSet       markSet;

        private static Map<String, CircleMarker> roundBorders  = new HashMap<>();
        private static Map<String, AreaMarker>   squareBorders = new HashMap<>();

        private static DynmapCommonAPIListener listener = new DynmapCommonAPIListener()
        {
            @Override
            public void apiEnabled(DynmapCommonAPI dynmapCommonAPI)
            {
                // FORGE: Old dynmap version check removed; 0.35 is obsolete by now
                api     = dynmapCommonAPI;
                markApi = api.getMarkerAPI();

                showAllBorders();
                Log.info("Successfully hooked into Dynmap for the ability to display borders");
            }
        };

        public static void register()
        {
            DynmapCommonAPIListener.register(listener);
        }
    }

    private static final int    LINE_WEIGHT  = 3;
    private static final double LINE_OPACITY = 1.0;
    private static final int    LINE_COLOR   = 0xFF0000;

    private static boolean enabled = false;

    public static void registerListener()
    {
        enabled = Loader.isModLoaded("Dynmap");

        if (enabled)
            Gateway.register();
        else
            Log.debug("Dynmap is not available; integration disabled");
    }

    /*
     * Re-rendering methods, used for updating trimmed chunks to show them as gone
     * TODO: Check if these are now working
     */

    public static void renderRegion(World world, CoordXZ coord)
    {
        if (!enabled) return;

        int y = (world != null) ? world.getHeight() : 255;
        int x = CoordXZ.regionToBlock(coord.x);
        int z = CoordXZ.regionToBlock(coord.z);
        Gateway.api.triggerRenderOfVolume(Worlds.getWorldName(world), x, 0, z, x + 511, y, z + 511);
    }

    public static void renderChunks(World world, List<CoordXZ> coords)
    {
        if (!enabled) return;

        int y = (world != null) ? world.getHeight() : 255;

        for (CoordXZ coord : coords)
            renderChunk(Worlds.getWorldName(world), coord, y);
    }

    public static void renderChunk(String worldName, CoordXZ coord, int maxY)
    {
        if (!enabled) return;

        int x = CoordXZ.chunkToBlock(coord.x);
        int z = CoordXZ.chunkToBlock(coord.z);
        Gateway.api.triggerRenderOfVolume(worldName, x, 0, z, x + 15, maxY, z + 15);
    }

    /*
     * Methods for displaying our borders on DynMap's world maps
     */

    public static void showAllBorders()
    {
        if (!enabled) return;

        // in case any borders are already shown
        removeAllBorders();

        if (!Config.isDynmapBorderEnabled())
        {
            // don't want to show the marker set in DynMap if our integration is disabled
            if (Gateway.markSet != null)
                Gateway.markSet.deleteMarkerSet();
            Gateway.markSet = null;
            return;
        }

        // make sure the marker set is initialized
        Gateway.markSet = Gateway.markApi.getMarkerSet("worldborder.markerset");
        if(Gateway.markSet == null)
            Gateway.markSet = Gateway.markApi.createMarkerSet("worldborder.markerset", "WorldBorder", null, false);
        else
            Gateway.markSet.setMarkerSetLabel("WorldBorder");

        Map<String, BorderData> borders = Config.getBorders();
        for( Entry<String, BorderData> stringBorderDataEntry : borders.entrySet() )
        {
            String     worldName = stringBorderDataEntry.getKey();
            BorderData border    = stringBorderDataEntry.getValue();

            showBorder(worldName, border);
        }
    }

    public static void showBorder(String worldName, BorderData border)
    {
        if (!enabled) return;

        if (!Config.isDynmapBorderEnabled()) return;

        if ((border.getShape() == null) ? Config.getShapeRound() : border.getShape())
            showRoundBorder(worldName, border);
        else
            showSquareBorder(worldName, border);
    }

    private static void showRoundBorder(String worldName, BorderData border)
    {
        if ( Gateway.squareBorders.containsKey(worldName) )
            removeBorder(worldName);

        CircleMarker marker = Gateway.roundBorders.get(worldName);
        if (marker == null)
        {
            marker = Gateway.markSet.createCircleMarker(
                "worldborder_" + worldName,
                Config.getDynmapMessage(),
                false, worldName,
                border.getX(), 64.0, border.getZ(),
                border.getRadiusX(), border.getRadiusZ(),
                true
            );

            marker.setLineStyle(LINE_WEIGHT, LINE_OPACITY, LINE_COLOR);
            marker.setFillStyle(0.0, 0x000000);
            Gateway.roundBorders.put(worldName, marker);
        }
        else
        {
            marker.setCenter(worldName, border.getX(), 64.0, border.getZ());
            marker.setRadius(border.getRadiusX(), border.getRadiusZ());
        }
    }

    private static void showSquareBorder(String worldName, BorderData border)
    {
        if ( Gateway.roundBorders.containsKey(worldName) )
            removeBorder(worldName);

        // corners of the square border
        double[] xVals = {border.getX() - border.getRadiusX(), border.getX() + border.getRadiusX()};
        double[] zVals = {border.getZ() - border.getRadiusZ(), border.getZ() + border.getRadiusZ()};

        AreaMarker marker = Gateway.squareBorders.get(worldName);
        if (marker == null)
        {
            marker = Gateway.markSet.createAreaMarker(
                "worldborder_" + worldName,
                Config.getDynmapMessage(),
                false, worldName, xVals, zVals, true
            );

            marker.setLineStyle(LINE_WEIGHT, LINE_OPACITY, LINE_COLOR);
            marker.setFillStyle(0.0, 0x000000);
            Gateway.squareBorders.put(worldName, marker);
        }
        else
            marker.setCornerLocations(xVals, zVals);
    }

    public static void removeAllBorders()
    {
        if (!enabled) return;

        for ( CircleMarker marker : Gateway.roundBorders.values() )
            marker.deleteMarker();
        Gateway.roundBorders.clear();

        for ( AreaMarker marker : Gateway.squareBorders.values() )
            marker.deleteMarker();
        Gateway.squareBorders.clear();
    }

    public static void removeBorder(String worldName)
    {
        if (!enabled) return;

        CircleMarker marker = Gateway.roundBorders.remove(worldName);
        if (marker != null)
            marker.deleteMarker();

        AreaMarker marker2 = Gateway.squareBorders.remove(worldName);
        if (marker2 != null)
            marker2.deleteMarker();
    }
}
