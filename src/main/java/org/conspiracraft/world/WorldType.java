package org.conspiracraft.world;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.util.Random;

public class WorldType {
    public Random rand() {
        return null;
    }
    public void renderCelestialBodies(MemoryStack stack) {}
    public Vector4f getSkylight() {return null;}
    public Vector3f getSun() {return null;}
    public void tick() {}
}
