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
import org.conspiracraft.world.shapes.Cube;
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
        Cube.generate(new Vector2i(), (World.size/2)+48, (World.height/2)-24, (World.size/2)+24, BlockTypes.STEEL_PLATING.id, 0, chunkSize/3);
        Cube.generate(new Vector2i(), (World.size/2)+48, (World.height/2)-24, (World.size/2)+48, BlockTypes.HAZARD.id, 0, chunkSize/3);
        for (int x = (World.size/2)+12-chunkSize; x <= (World.size/2)+12+chunkSize; x++) {
            for (int z = (World.size / 2) + 12-chunkSize; z <= (World.size / 2) + 54+chunkSize; z++) {
                if (x > (World.size/2)+6 && x < (World.size/2)+18) {
                    World.setBlock(x, ((World.height-chunkSize)/2)-10, z, BlockTypes.STEEL_PLATING.id, 0);
                } else {
                    if (x == (World.size/2)+12-chunkSize || x == (World.size/2)+12+chunkSize) {
                        for (int y = ((World.height-chunkSize)/2)-11; y >= ((World.height-chunkSize)/2)-31; y--) {
                            World.setBlock(x, y, z, BlockTypes.GLASS.id, 0);
                        }
                    } else {
                        World.setBlock(x, ((World.height-chunkSize)/2)-11, z, BlockTypes.GLASS.id, 0);
                    }
                    if (x == (World.size/2)+6 || x == (World.size/2)+18) {
                        World.setBlock(x, ((World.height-chunkSize)/2)-32, z, BlockTypes.HAZARD.id, 0);
                    } else {
                        World.setBlock(x, ((World.height-chunkSize)/2)-32, z, BlockTypes.STEEL_PLATING.id, 0);
                    }
                }
                World.setBlock(x, ((World.height-chunkSize)/2)-33, z, BlockTypes.STEEL_PLATING.id, 0);
            }
        }
        for (int x = (World.size/2)+10; x <= (World.size/2)+14; x++) {
            for (int z = (World.size/2)+10; z <= (World.size/2)+14; z++) {
                World.setBlock(x, ((World.height-chunkSize)/2)-9, z, BlockTypes.HAZARD.id, 0);
                World.setBlock(x, ((World.height-chunkSize)/2)-10, z, BlockTypes.HAZARD.id, 0);
                World.setBlock(x, ((World.height-chunkSize)/2)-33, z, BlockTypes.HAZARD.id, 0);
                World.setBlock(x, ((World.height-chunkSize)/2)-34, z, BlockTypes.HAZARD.id, 0);
                if (x > (World.size/2)+10 && x < (World.size/2)+14 && z > (World.size/2)+10 && z < (World.size/2)+14) {
                    for (int y = ((World.height - chunkSize) / 2) - 32; y <= ((World.height - chunkSize) / 2) - 11; y++) {
                        World.setBlock(x, y, z, BlockTypes.STEEL_FRAME.id, 0);
                    }
                }
            }

            for (int z = (World.size/2)+50; z <= (World.size/2)+54; z++) {
                World.setBlock(x, ((World.height-chunkSize)/2)-9, z, BlockTypes.HAZARD.id, 0);
                World.setBlock(x, ((World.height-chunkSize)/2)-10, z, BlockTypes.HAZARD.id, 0);
                World.setBlock(x, ((World.height-chunkSize)/2)-33, z, BlockTypes.HAZARD.id, 0);
                World.setBlock(x, ((World.height-chunkSize)/2)-34, z, BlockTypes.HAZARD.id, 0);
                if (x > (World.size/2)+10 && x < (World.size/2)+14 && z > (World.size/2)+50 && z < (World.size/2)+54) {
                    for (int y = ((World.height - chunkSize) / 2) - 32; y <= ((World.height - chunkSize) / 2) - 11; y++) {
                        World.setBlock(x, y, z, BlockTypes.STEEL_FRAME.id, 0);
                    }
                }
            }
        }
    }
    public float gravity() {return 0.0034f;}
    public float getFogginess() {return -1.f;}
    public Vector4f getAtmosphereColor() {return new Vector4f(0);}
    public Vector4f getNightAtmosphereColor() {return new Vector4f(0);}
    public Vector4f getSunsetAtmosphereColor() {return new Vector4f(0);}
    public Vector4f getDeepSunsetAtmosphereColor() {return new Vector4f(0);}
    public final Vector3f skylightMul = new Vector3f(1);
    public Vector3f getSkylightMul() {return skylightMul;}
}
