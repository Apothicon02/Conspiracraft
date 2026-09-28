package org.conspiracraft.world;

import org.conspiracraft.blocks.types.BlockTypes;

import java.util.ArrayList;

public class Biomes {
    public static ArrayList<Biome> biomesTemp = new ArrayList<Biome>();

    public static final Biome LAKE = create(1, BlockTypes.WET_SAND.id, 0, 7, BlockTypes.WET_SAND.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome TEMPERATE = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome TAIGA = create(1, BlockTypes.GRASS.id, 1, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome CHERRY_GROVE = create(1, BlockTypes.GRASS.id, 3, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome REDWOOD_FOREST = create(1, BlockTypes.GRASS.id, 1, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome VOLCANIC_TAIGA = create(1, BlockTypes.GRASS.id, 1, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome VOLCANIC_SNOWY_TAIGA = create(3, BlockTypes.SNOW.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome SNOWY_PEAK = create(3, BlockTypes.SNOW.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome SNOWY_TAIGA = create(3, BlockTypes.SNOW.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome DESERT = create(1, BlockTypes.SAND.id, 0, 7, BlockTypes.SAND.id, 0, BlockTypes.SANDSTONE.id, 0);
    public static final Biome PALMY_PLAINS = create(1, BlockTypes.GRASS.id, 3, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome TROPICAL_ISLAND = create(1, BlockTypes.GRASS.id, 3, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome BEACH = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome SAVANNA = create(1, BlockTypes.GRASS.id, 2, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome BADLANDS = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome OASIS = create(1, BlockTypes.WET_SAND.id, 0, 7, BlockTypes.WET_SAND.id, 0, BlockTypes.SANDSTONE.id, 0);
    public static final Biome RAINFOREST = create(1, BlockTypes.GRASS.id, 3, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome PALMY_HILLS = create(1, BlockTypes.GRASS.id, 3, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome POND = create(1, BlockTypes.MUD.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome ROOFED_FOREST = create(1, BlockTypes.GRASS.id, 1, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome ROOFED_FOREST_HILLS = create(1, BlockTypes.GRASS.id, 1, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome FLOWERY_FIELD = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome BIRCH_PLAINS = create(1, BlockTypes.GRASS.id, 3, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome FROZEN_LAKE = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome RAINY_POND = create(1, BlockTypes.MUD.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome ROOFED_POND = create(1, BlockTypes.MUD.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome MARB_HIGHLANDS = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome MARB_CRATER = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome VERA_PLAINS = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome VERA_HILLS = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome AKSALA = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome AKSALA_PEAKS = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome LAZULI_DUNES = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome LAZULI_BADLANDS = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);
    public static final Biome LAZULI_RIDGES = create(1, BlockTypes.GRASS.id, 0, 4, BlockTypes.DIRT.id, 0, BlockTypes.STONE.id, 0);

    public static final Biome[] biomes = biomesTemp.toArray(new Biome[0]);
    public static Biome create(int surfaceDepth, int surfaceBlockType, int surfaceBlockSubtype, int subsurfaceDepth, int subsurfaceBlockType, int subsurfaceBlockSubtype, int groundBlockType, int groundBlockSubtype) {
        Biome biome = new Biome((byte) (biomesTemp.size()), surfaceDepth, surfaceBlockType, surfaceBlockSubtype, subsurfaceDepth, subsurfaceBlockType, subsurfaceBlockSubtype, groundBlockType, groundBlockSubtype);
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
        } else if (biome == FROZEN_LAKE || biome == SNOWY_PEAK || biome == SNOWY_TAIGA || biome == VOLCANIC_SNOWY_TAIGA) {
            type = BlockTypes.SNOW.id;
            subtype = 0;
        } else if (biome == LAKE || biome == OASIS || biome == ROOFED_POND) {
            type = BlockTypes.WET_SAND.id;
            subtype = 0;
        } else if (biome == POND || biome == RAINY_POND) {
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
