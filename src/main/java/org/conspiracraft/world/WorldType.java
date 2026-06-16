package org.conspiracraft.world;
import org.conspiracraft.Main;
import org.conspiracraft.effects.Effect;
import org.conspiracraft.effects.Lightning;
import org.conspiracraft.space.Planet;
import org.conspiracraft.space.StarSystem;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.file.Path;

import static org.conspiracraft.world.World.effects;
import static org.conspiracraft.world.World.height;

public class WorldType {
    public float getLongitude() {return 0.f;}
    public Path getWorldPath() {return Path.of("none");}
    public Planet getPlanet(){return null;}
    public static Vector3f nearestLightning = new Vector3f();
    public static Vector4f oliviusColor = new Vector4f(0.34f, 0.949f, 0.475f, 1);
    public Vector4f getSkylight() {
        nearestLightning.set(-100000);
        for (Effect effect : effects) {
            if (effect instanceof Lightning lightning) {
                Vector3f lightningPos = lightning.pos;
                if (Main.player.pos.distance(lightningPos) <= Main.player.pos.distance(nearestLightning)) {
                    nearestLightning.set(lightningPos);
                }
            }
        }
        if (nearestLightning.x() >= 0) {
            skylightMul.set(1.f, 0.95f, 0.f);
            return new Vector4f(nearestLightning.x(), nearestLightning.y(), nearestLightning.z(), 4);
        }
        skylightMul.set(1);
        Vector4f skylight = new Vector4f(StarSystem.relativePos, 1);
        if (skylight.y() <= 0) {
            skylight.y = 0;
            for (Planet planet : StarSystem.planets) {
                if (planet.rotatedPos.y() > skylight.y()) {
                    float brightness = planet.scale;
                }
            }
        }
        if (skylight.y() <= 0) {
            return new Vector4f(0);
        } else {
            return new Vector4f(skylight.x(), Math.max(height, skylight.y()), skylight.z(), skylight.w());
        }
    }
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
