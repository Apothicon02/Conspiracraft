package org.conspiracraft.world.types;
import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.effects.Effect;
import org.conspiracraft.effects.Lightning;
import org.conspiracraft.space.Planet;
import org.conspiracraft.space.StarSystem;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.World;
import org.conspiracraft.world.shapes.BevelledCube;
import org.conspiracraft.world.shapes.Blob;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.file.Path;
import java.util.Random;

import static org.conspiracraft.world.World.*;
import static org.conspiracraft.world.World.chunkSize;
import static org.conspiracraft.world.World.packLodPos;

public class WorldType {
    public WorldType space() {return null;}
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
        if (space() == null) {return skylight;}
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
    public void generate() throws InterruptedException {
        for (int cX = 0; cX < World.sizeChunks; cX++) {
            for (int cZ = 0; cZ < World.sizeChunks; cZ++) {
                for (int cY = 0; cY < World.heightChunks; cY++) {
                    int packedCP = World.packChunkPos(cX, cY, cZ);
                    World.chunks[packedCP] = new Chunk(packedCP);
                }
            }
        }
        BevelledCube.generate(World.size/2, World.height/2, World.size/2, BlockTypes.MARBLE.id, 0, chunkSize);
        BevelledCube.generate(World.size/2, (World.height/2)+4, (World.size/2)+8, BlockTypes.STONE.id, 0, chunkSize);
        BevelledCube.generate((World.size/2)-5, (World.height/2)-2, World.size/2, BlockTypes.MARBLE.id, 0, chunkSize);
        BevelledCube.generate((World.size/2)+3, World.height/2, (World.size/2)+5, BlockTypes.STONE.id, 0, chunkSize);
        final Random rand = new Random(World.seed);
        float chance = 0.0001f;
        for (int x = (size/2)-chunkSize; x < (size/2)+chunkSize; x++) {
            for (int z = (size/2)-chunkSize; z < (size/2)+chunkSize; z++) {
                for (int y = (height/2)-chunkSize; y < (height/2)+chunkSize; y++) {
                    if (rand.nextFloat() < chance) {
                        chance = 0.0001f;
                        Blob.generate(new Vector2i(0), x, y, z, 0, 0, (int) (2 + (rand.nextFloat() * 5)));
                    } else {
                        chance += 0.00003f;
                    }
                }
            }
        }
    }
    public float gravity() {return 0.f;}
    public float getFogginess() {return -1.f;}
    public Vector4f getAtmosphereColor() {return new Vector4f(0);}
    public Vector4f getNightAtmosphereColor() {return new Vector4f(0);}
    public Vector4f getSunsetAtmosphereColor() {return new Vector4f(0);}
    public Vector4f getDeepSunsetAtmosphereColor() {return new Vector4f(0);}
    public final Vector3f skylightMul = new Vector3f(1);
    public Vector3f getSkylightMul() {return skylightMul;}
}
