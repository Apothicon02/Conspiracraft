package org.conspiracraft.world;

public record Biome(byte id, int surfaceDepth, int surfaceBlockType, int surfaceBlockSubtype, int subsurfaceDepth, int subsurfaceBlockType, int subsurfaceBlockSubtype, int groundBlockType, int groundBlockSubtype) {}
