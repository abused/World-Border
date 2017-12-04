package com.wimbli.WorldBorder;

import com.wimbli.WorldBorder.forge.Location;
import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Worlds;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.Arrays;
import java.util.LinkedHashSet;

public class BorderData
{
    // the main data interacted with
    private double x       = 0;
    private double z       = 0;
    private int    radiusX = 0;
    private int    radiusZ = 0;

    private Boolean shapeRound = null;
    private boolean wrapping   = false;

    // some extra data kept handy for faster border checks
    private double maxX;
    private double minX;
    private double maxZ;
    private double minZ;
    private double radiusXSquared;
    private double radiusZSquared;
    private double DefiniteRectangleX;
    private double DefiniteRectangleZ;
    private double radiusSquaredQuotient;

    // <editor-fold desc="Constructors">
    public BorderData(double x, double z, int radiusX, int radiusZ, Boolean shapeRound, boolean wrap)
    {
        setData(x, z, radiusX, radiusZ, shapeRound, wrap);
    }

    public BorderData(double x, double z, int radiusX, int radiusZ)
    {
        setData(x, z, radiusX, radiusZ, null);
    }

    public BorderData(double x, double z, int radiusX, int radiusZ, Boolean shapeRound)
    {
        setData(x, z, radiusX, radiusZ, shapeRound);
    }

    public BorderData(double x, double z, int radius)
    {
        setData(x, z, radius, null);
    }

    public BorderData(double x, double z, int radius, Boolean shapeRound)
    {
        setData(x, z, radius, shapeRound);
    }
    // </editor-fold>

    // <editor-fold desc="setData overloads">
    public final void setData(double x, double z, int radiusX, int radiusZ, Boolean shapeRound, boolean wrap)
    {
        this.x = x;
        this.z = z;
        this.shapeRound = shapeRound;
        this.wrapping = wrap;
        this.setRadiusX(radiusX);
        this.setRadiusZ(radiusZ);
    }

    public final void setData(double x, double z, int radiusX, int radiusZ, Boolean shapeRound)
    {
        setData(x, z, radiusX, radiusZ, shapeRound, false);
    }

    public final void setData(double x, double z, int radius, Boolean shapeRound)
    {
        setData(x, z, radius, radius, shapeRound, false);
    }
    // </editor-fold>

    //<editor-fold desc="Getters and setters">
    public double getX()
    {
        return x;
    }

    public void setX(double x)
    {
        this.x = x;
        this.maxX = x + radiusX;
        this.minX = x - radiusX;
    }

    public double getZ()
    {
        return z;
    }

    public void setZ(double z)
    {
        this.z = z;
        this.maxZ = z + radiusZ;
        this.minZ = z - radiusZ;
    }

    public int getRadiusX()
    {
        return radiusX;
    }

    public int getRadiusZ()
    {
        return radiusZ;
    }

    public void setRadiusX(int radiusX)
    {
        this.radiusX = radiusX;
        this.maxX    = x + radiusX;
        this.minX    = x - radiusX;

        this.radiusXSquared        = (double) radiusX * (double) radiusX;
        this.radiusSquaredQuotient = this.radiusXSquared / this.radiusZSquared;
        this.DefiniteRectangleX    = Math.sqrt(.5 * this.radiusXSquared);
    }

    public void setRadiusZ(int radiusZ)
    {
        this.radiusZ = radiusZ;
        this.maxZ    = z + radiusZ;
        this.minZ    = z - radiusZ;

        this.radiusZSquared        = (double) radiusZ * (double) radiusZ;
        this.radiusSquaredQuotient = this.radiusXSquared / this.radiusZSquared;
        this.DefiniteRectangleZ    = Math.sqrt(.5 * this.radiusZSquared);
    }

    public void setRadius(int radius)
    {
        setRadiusX(radius);
        setRadiusZ(radius);
    }

    public Boolean getShape()
    {
        return shapeRound;
    }

    public void setShape(Boolean shapeRound)
    {
        this.shapeRound = shapeRound;
    }

    public boolean getWrapping()
    {
        return wrapping;
    }

    public void setWrapping(boolean wrap)
    {
        this.wrapping = wrap;
    }
    //</editor-fold>

    public BorderData copy()
    {
        return new BorderData(x, z, radiusX, radiusZ, shapeRound, wrapping);
    }

    @Override
    public String toString()
    {
        return String.format("radius %s at X: %s Z: %s%s%s",
            (radiusX == radiusZ) ? radiusX : radiusX + "*" + radiusZ,
            Config.COORD_FORMAT.format(x),
            Config.COORD_FORMAT.format(z),
            shapeRound != null
                ? String.format( " (shape override: %s)", Config.getShapeName(shapeRound) )
                : "",
            wrapping ? " (wrapping)" : ""
        );
    }

    /** This algorithm of course needs to be fast, since it will be run very frequently */
    public boolean insideBorder(double xLoc, double zLoc, boolean round)
    {
        // if this border has a shape override set, use it
        if (shapeRound != null)
            round = shapeRound;

        // square border
        if (!round)
            return !(xLoc < minX || xLoc > maxX || zLoc < minZ || zLoc > maxZ);

        // round border
        else
        {
            // elegant round border checking algorithm is from rBorder by Reil with almost no changes, all credit to him for it
            double X = Math.abs(x - xLoc);
            double Z = Math.abs(z - zLoc);

            if (X < DefiniteRectangleX && Z < DefiniteRectangleZ)
                return true;	// Definitely inside
            else if (X >= radiusX || Z >= radiusZ)
                return false;	// Definitely outside
            else if (X * X + Z * Z * radiusSquaredQuotient < radiusXSquared)
                return true;	// After further calculation, inside
            else
                return false;	// Apparently outside, then
        }
    }
    public boolean insideBorder(double xLoc, double zLoc)
    {
        return insideBorder(xLoc, zLoc, Config.getShapeRound());
    }

    public boolean insideBorder(Location loc)
    {
        return insideBorder(loc.posX, loc.posZ, Config.getShapeRound());
    }

    public boolean insideBorder(CoordXZ coord, boolean round)
    {
        return insideBorder(coord.x, coord.z, round);
    }

    public boolean insideBorder(CoordXZ coord)
    {
        return insideBorder(coord.x, coord.z, Config.getShapeRound());
    }

    public Location correctedPosition(Location loc, boolean round, boolean flying)
    {
        // if this border has a shape override set, use it
        if (shapeRound != null)
            round = shapeRound;

        double xLoc  = loc.posX;
        double yLoc  = loc.posY;
        double zLoc  = loc.posZ;
        double knock = Config.getKnockBack();

        // Make sure knockback is not too big for this border
        if (knock >= radiusX * 2 || knock >= radiusZ * 2)
        {
            Log.warn("Knockback %.2f is too big for border. Defaulting to 3.0.", knock);
            knock = 3.0;
        }

        // square border
        if (!round)
        {
            if (wrapping)
            {
                if (xLoc <= minX)
                    xLoc = maxX - knock;
                else if (xLoc >= maxX)
                    xLoc = minX + knock;
                if (zLoc <= minZ)
                    zLoc = maxZ - knock;
                else if (zLoc >= maxZ)
                    zLoc = minZ + knock;
            }
            else
            {
                if (xLoc <= minX)
                    xLoc = minX + knock;
                else if (xLoc >= maxX)
                    xLoc = maxX - knock;
                if (zLoc <= minZ)
                    zLoc = minZ + knock;
                else if (zLoc >= maxZ)
                    zLoc = maxZ - knock;
            }
        }

        // round border
        else
        {
            // algorithm originally from: http://stackoverflow.com/q/300871/3354920
            // modified by Lang Lukas to support elliptical border shape

            // Transform the ellipse to a circle with radius 1 (we need to transform the point the same way)
            double dX = xLoc - x;
            double dZ = zLoc - z;
            // Distance of the untransformed point from the center
            double dU = Math.sqrt(dX *dX + dZ * dZ);
            // Distance of the transformed point from the center
            double dT = Math.sqrt(dX *dX / radiusXSquared + dZ * dZ / radiusZSquared);
            // "Correction" factor for the distances
            double f  = (1 / dT - knock / dU);

            if (wrapping)
            {
                xLoc = x - dX * f;
                zLoc = z - dZ * f;
            }
            else
            {
                xLoc = x + dX * f;
                zLoc = z + dZ * f;
            }
        }

        int ixLoc  = Location.locToBlock(xLoc);
        int izLoc  = Location.locToBlock(zLoc);
        int icxLoc = CoordXZ.blockToChunk(ixLoc);
        int icZLoc = CoordXZ.blockToChunk(izLoc);

        // Make sure the chunk we're checking in is actually loaded
        // TODO: should this be here?
        Chunk tChunk = loc.world.getChunkFromBlockCoords(new BlockPos(ixLoc, 0, izLoc));
        if (!tChunk.isLoaded())
            loc.world.getChunkProvider().loadChunk(icxLoc, icZLoc);

        yLoc = getSafeY(loc.world, ixLoc, Location.locToBlock(yLoc), izLoc, flying);
        if (yLoc == -1)
            return null;

        return new Location(loc.world, Math.floor(xLoc) + 0.5, yLoc, Math.floor(zLoc) + 0.5, loc.yaw, loc.pitch);
    }

    public Location correctedPosition(Location loc, boolean round)
    {
        return correctedPosition(loc, round, false);
    }

    public Location correctedPosition(Location loc)
    {
        return correctedPosition(loc, Config.getShapeRound(), false);
    }

    //these material IDs are acceptable for places to teleport player; breathable blocks and water
    public static final LinkedHashSet<Integer> safeOpenBlocks = new LinkedHashSet<>(Arrays.asList(
        new Integer[] {0, 6, 8, 9, 27, 28, 30, 31, 32, 37, 38, 39, 40, 50, 55, 59, 63, 64, 65, 66, 68, 69, 70, 71, 72, 75, 76, 77, 78, 83, 90, 93, 94, 96, 104, 105, 106, 115, 131, 132, 141, 142, 149, 150, 157, 171}
    ));

    //these material IDs are ones we don't want to drop the player onto, like cactus or lava or fire or activated Ender portal
    public static final LinkedHashSet<Integer> painfulBlocks = new LinkedHashSet<>(Arrays.asList(
        new Integer[] {10, 11, 51, 81, 119}
    ));

    // check if a particular spot consists of 2 breathable blocks over something relatively solid
    private boolean isSafeSpot(WorldServer world, int X, int Y, int Z, boolean flying)
    {
        boolean safe = safeOpenBlocks.contains( Worlds.getBlockID(world, X, Y, Z) )		// target block open and safe
                    && safeOpenBlocks.contains( Worlds.getBlockID(world, X, Y + 1, Z) );	// above target block open and safe

        if (!safe || flying)
            return safe;

        int below = Worlds.getBlockID(world, X, Y - 1, Z);

        return
            !(below == 7 && world.provider.getDimension() == -1)                // try not to place player above bedrock in nether
            && (!safeOpenBlocks.contains(below) || below == 8 || below == 9) // below target block not open/breathable (so presumably solid), or is water
            && !painfulBlocks.contains(below);                               // below target block not painful
    }

    private static final int limBot = 1;

    // find closest safe Y position from the starting position
    public double getSafeY(WorldServer world, int X, int Y, int Z, boolean flying)
    {
        final int limTop = world.getHeight() - 2;
        // Expanding Y search method adapted from Acru's code in the Nether plugin

        for(int y1 = Y, y2 = Y; (y1 > limBot) || (y2 < limTop); y1--, y2++)
        {
            // Look below.
            if (y1 > limBot)
            if ( isSafeSpot(world, X, y1, Z, flying) )
                return (double) y1;

            // Look above.
            if (y2 < limTop && y2 != y1)
            if ( isSafeSpot(world, X, y2, Z, flying) )
                return (double) y2;
        }

        // no safe Y location?!?!? Must be a rare spot in a Nether world or something
        return -1.0;
    }

    @Override
    public boolean equals(Object obj)
    {
        if      (this == obj)
            return true;
        else if ( obj == null || obj.getClass() != this.getClass() )
            return false;

        BorderData test = (BorderData) obj;
        return test.x == this.x
            && test.z == this.z
            && test.radiusX == this.radiusX
            && test.radiusZ == this.radiusZ;
    }

    @Override
    public int hashCode()
    {
        return ((int) (this.x * 10) << 4)
            + (int) this.z
            + (this.radiusX << 2)
            + (this.radiusZ << 3);
    }
}
