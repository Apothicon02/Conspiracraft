package org.conspiracraft.world;
import org.conspiracraft.space.Planet;
import org.conspiracraft.space.StarSystem;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.file.Path;

public class WorldType {
    public float getLongitude() {return 0.f;}
    public Path getWorldPath() {return Path.of("none");}
    public Planet getPlanet(){return null;}
    public Vector4f getSkylight() {return new Vector4f(StarSystem.pos, 1);}
    public void tick() {}
    public void generate() throws InterruptedException {}
    public float gravity() {return 0.f;}
    public float getFogginess() {return -1.f;}
    public Vector4f getAtmosphereColor() {return new Vector4f(0);}
    public Vector4f getNightAtmosphereColor() {return new Vector4f(0);}
    public Vector4f getSunsetAtmosphereColor() {return new Vector4f(0);}
    public Vector4f getDeepSunsetAtmosphereColor() {return new Vector4f(0);}
    public final Vector3f skylightMul = new Vector3f(1);
    public Vector3f getSkylightMul() {return skylightMul;}
}
