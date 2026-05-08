package org.conspiracraft.world;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.file.Path;
import java.util.Random;

public class WorldType {
    public Path getWorldPath() {return Path.of("none");}
    public void renderCelestialBodies(MemoryStack stack) {}
    public Vector4f getSkylight() {return null;}
    public Vector3f getSun() {return null;}
    public void tick() {}
    public void generate() throws InterruptedException {}
    public float gravity() {return 0.1f;}
}
