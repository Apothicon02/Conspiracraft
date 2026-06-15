package org.conspiracraft.world;
import org.conspiracraft.space.Planet;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.file.Path;

public class WorldType {
    public Path getWorldPath() {return Path.of("none");}
    public Planet getPlanet(){return null;}
    public void renderCelestialBodies(MemoryStack stack) {}
    public Vector4f getSkylight() {return null;}
    public void tick() {}
    public void generate() throws InterruptedException {}
    public float gravity() {return 0.1f;}
    public float getFogginess() {return 1.f;}
    public Vector4f getAtmosphereColor() {return null;}
    public Vector4f getNightAtmosphereColor() {return null;}
    public Vector4f getSunsetAtmosphereColor() {return null;}
    public Vector4f getDeepSunsetAtmosphereColor() {return null;}
    public final Vector3f skylightMul = new Vector3f(1);
    public Vector3f getSkylightMul() {return skylightMul;}
}
