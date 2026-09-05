package org.conspiracraft.world;

public record RegionNoises(double[] continents, double[] vegetation, double[] dunes, double[] detail, double[] plains, double[] hills, double[] whiteNoise) {
    public RegionNoises(int size) {
        this(new double[size], new double[size], new double[size], new double[size], new double[size], new double[size], new double[size]);
    }
}
