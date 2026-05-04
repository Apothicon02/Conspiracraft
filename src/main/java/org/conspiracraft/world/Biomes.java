package org.conspiracraft.world;

import java.util.ArrayList;

public class Biomes {
    public static ArrayList<Biome> biomes = new ArrayList<Biome>();

    public static Biome LAKE = create();
    public static Biome TEMPERATE = create();
    public static Biome TAIGA = create();
    public static Biome CHERRY_GROVE = create();
    public static Biome SNOWY_PEAK = create();
    public static Biome SNOWY_TAIGA = create();
    public static Biome DESERT = create();
    public static Biome TROPICAL_ISLAND = create();

    public static Biome create() {
        Biome biome = new Biome((byte) (biomes.size()));
        biomes.addLast(biome);
        return biome;
    }
}
