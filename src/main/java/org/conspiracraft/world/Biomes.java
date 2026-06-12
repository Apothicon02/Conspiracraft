package org.conspiracraft.world;

import org.conspiracraft.blocks.types.BlockTypes;

import java.util.ArrayList;

public class Biomes {
    public static ArrayList<Biome> biomesTemp = new ArrayList<Biome>();

    public static final Biome LAKE = create();
    public static final Biome TEMPERATE = create();
    public static final Biome TAIGA = create();
    public static final Biome CHERRY_GROVE = create();
    public static final Biome REDWOOD_FOREST = create();
    public static final Biome VOLCANIC_TAIGA = create();
    public static final Biome VOLCANIC_SNOWY_TAIGA = create();
    public static final Biome SNOWY_PEAK = create();
    public static final Biome SNOWY_TAIGA = create();
    public static final Biome DESERT = create();
    public static final Biome PALMY_PLAINS = create();
    public static final Biome TROPICAL_ISLAND = create();
    public static final Biome BEACH = create();
    public static final Biome SAVANNA = create();
    public static final Biome BADLANDS = create();
    public static final Biome OASIS = create();
    public static final Biome RAINFOREST = create();
    public static final Biome PALMY_HILLS = create();
    public static final Biome POND = create();
    public static final Biome ROOFED_FOREST = create();
    public static final Biome ROOFED_FOREST_HILLS = create();
    public static final Biome BIRCH_PLAINS = create();
    public static final Biome MARB_HIGHLANDS = create();
    public static final Biome MARB_CRATER = create();
    public static final Biome VERA_PLAINS = create();
    public static final Biome VERA_HILLS = create();

    public static final Biome[] biomes = biomesTemp.toArray(new Biome[0]);
    public static Biome create() {
        Biome biome = new Biome((byte) (biomesTemp.size()));
        biomesTemp.addLast(biome);
        return biome;
    }

    public static int getSurfaceBlock(final byte biomeId, final short elevation, final int y) {
        final Biome biome = biomes[biomeId];
        int type;
        int subtype;
        if (elevation <= World.seaLevel) {
            type = BlockTypes.WET_SAND.id;
            subtype = 0;
        } else if (elevation <= World.seaLevel+2+(biome == BEACH ? 1 : 0) || biome == DESERT) {
            type = BlockTypes.SAND.id;
            subtype = 0;
        } else if (biome == SNOWY_PEAK || biome == SNOWY_TAIGA || biome == VOLCANIC_SNOWY_TAIGA) {
            type = BlockTypes.SNOW.id;
            subtype = 0;
        } else if (biome == LAKE || biome == OASIS) {
            type = BlockTypes.WET_SAND.id;
            subtype = 0;
        } else if (biome == POND) {
            type = BlockTypes.MUD.id;
            subtype = 0;
        } else if (biome == VOLCANIC_TAIGA || biome == TAIGA || biome == REDWOOD_FOREST || biome == ROOFED_FOREST || biome == ROOFED_FOREST_HILLS) {
            type = y >= elevation ? BlockTypes.GRASS.id : BlockTypes.DIRT.id;
            subtype = y >= elevation ? 1 : 0;
        } else if (biome == SAVANNA) {
            type = y >= elevation ? BlockTypes.GRASS.id : BlockTypes.DRY_MUD.id;
            subtype = y >= elevation ? 2 : 0;
        } else if (biome == BADLANDS) {
            type = BlockTypes.RED_SAND.id;
            subtype = 0;
        } else if (biome == TROPICAL_ISLAND || biome == PALMY_PLAINS || biome == PALMY_HILLS || biome == RAINFOREST || biome == BIRCH_PLAINS || biome == CHERRY_GROVE) {
            type = y >= elevation ? BlockTypes.GRASS.id : BlockTypes.DIRT.id;
            subtype = y >= elevation ? 3 : 0;
        } else {
            type = y >= elevation ? BlockTypes.GRASS.id : BlockTypes.DIRT.id;
            subtype = 0;
        }
        return Chunk.packInts(type, subtype);
    }

    public static int getBadlandsBands(int y) {
        y/=2;
        return (y&5) == 0 ? BlockTypes.SANDSTONE.id : ((y&2) == 0 ? BlockTypes.ORANGE_SANDSTONE.id : BlockTypes.RED_SANDSTONE.id);
    }
}
