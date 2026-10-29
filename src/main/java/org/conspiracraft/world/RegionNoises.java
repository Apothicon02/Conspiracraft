package org.conspiracraft.world;

public record RegionNoises(int width, double[] continents, double[] vegetation, double[] dunes, double[] detail, double[] plains, double[] hills, double[] whiteNoise, double[] ridges, double[] geographic, double[] biome) {
    public RegionNoises(int width) {
        int size = width*width;
        this(width, new double[size], new double[size], new double[size], new double[size], new double[size], new double[size], new double[size], new double[size], new double[size], new double[size]);
    }
}
