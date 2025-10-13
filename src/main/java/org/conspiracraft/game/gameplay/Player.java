package org.conspiracraft.game.gameplay;

import org.conspiracraft.Main;
import org.conspiracraft.engine.Camera;
import org.conspiracraft.engine.Utils;
import org.conspiracraft.game.Renderer;
import org.conspiracraft.game.audio.*;
import org.conspiracraft.game.blocks.Tags;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.World;
import org.joml.*;
import org.lwjgl.openal.AL10;

import java.io.IOException;
import java.lang.Math;

public class Player {
    private final Camera camera = new Camera();
    public final Vector3f oldCamOffset = new Vector3f();
    public final Source breakingSource;
    public final Source jumpSource;
    public final Source passthroughSource;
    public final Source swimSource;
    public final Source splashSource;
    public final Source submergeSource;
    public final Source waterFlowingSource;
    public final Source magmaSource;
    public final Source windSource;
    public float waterFlow = 0f;
    public final Source musicSource;
    public static float scale = 1f;
    public static float baseEyeHeight = 1.625f*scale;
    public static float eyeHeight = baseEyeHeight;
    public static float baseHeight = eyeHeight+(0.175f*scale);
    public static float height = baseHeight;
    public static float width = 0.4f*scale;
    public Vector3i blockPos;
    public Vector3f pos;
    public Vector3f oldPos;
    public Vector3f vel = new Vector3f(0f);
    public Vector3f movement = new Vector3f(0f);
    public float bounciness = 0.66f;
    public float friction = 0.75f;
    public float grav = 0.05f;
    public float baseSpeed = Math.max(0.15f, 0.15f*scale);
    public float speed = baseSpeed;
    public float jumpStrength = Math.max(0.33f, 0.33f*scale);
    public long lastJump = 1000;
    public long jump = 0;
    public int reach = 50;
    public float reachAccuracy = 200;
    public boolean creative = false;
    public boolean crawling = false;
    public boolean crouching = false;
    public boolean sprint = false;
    public boolean superSprint = false;
    public boolean forward = false;
    public boolean backward = false;
    public boolean rightward = false;
    public boolean leftward = false;
    public boolean upward = false;
    public boolean downward = false;
    public boolean flying = true;
    public int[] stack = new int[32];
    public Vector2i blockOn = new Vector2i(0);
    public Vector2i blockIn = new Vector2i(0);
    public Vector2i blockBreathing = new Vector2i(0);
    boolean submerged = false;

    public Player(Vector3f newPos) {
        breakingSource = new Source(newPos, 1, 1, 0, 1);
        jumpSource = new Source(newPos, 1, 1, 0, 0);
        passthroughSource = new Source(newPos, 1, 1, 0, 0);
        swimSource = new Source(newPos, 0.5f, 1, 0, 0);
        splashSource = new Source(newPos, 1, 1, 0, 0);
        submergeSource = new Source(newPos, 1, 1, 0, 0);
        waterFlowingSource = new Source(newPos, 0, 1, 0, 1);
        windSource = new Source(newPos, 0, 1, 0, 1);
        magmaSource = new Source(newPos, 0, 1, 0, 1);
        musicSource = new Source(newPos, 0.15f, 1, 0, 0);
        setPos(newPos);
        oldPos = newPos;
    }

    public int[] getData() {
        float[] cam = new float[16];
        camera.getViewMatrixWithoutPitch().get(cam);
        int[] data = new int[91];
        for (int i = 0; i < 16; i++) {
            data[i] = (int)(cam[i]*1000);
        }
        data[16] = (int)(camera.pitch.x*1000);
        data[17] = (int)(camera.pitch.y*1000);
        data[18] = (int)(camera.pitch.z*1000);
        data[19] = (int)(camera.pitch.w*1000);
        float[] posVelData = {pos.x, pos.y, pos.z, vel.x, vel.y, vel.z};
        for (int i = 20; i < 26; i++) {
            data[i] = (int)(posVelData[i-20]*1000);
        }
        data[26] = flying ? 1 : 0;
        for (int i = 27; i < 59; i++) {
            data[i] = stack[i-27];
        }
        return data;
    }

    public boolean setSolidBlockOn = false;
    public boolean solid(float x, float y, float z, boolean recordFriction, boolean recordBounciness) {
        Vector2i block = World.getBlock(x, y, z);
        if (block != null) {
            int typeId = block.x;
            if (BlockTypes.blockTypeMap.get(typeId).blockProperties.isCollidable) {
                int cornerData = World.getCorner((int) x, (int) y, (int) z);
                int cornerIndex = (y < (int)(y)+0.5 ? 0 : 4) + (z < (int)(z)+0.5 ? 0 : 2) + (x < (int)(x)+0.5 ? 0 : 1);
                int temp = cornerData;
                temp &= (~(1 << (cornerIndex - 1)));
                if (temp == cornerData) {
                    if (Renderer.collisionData[(1024 * ((typeId * 8) + (int) ((x - Math.floor(x)) * 8))) + (block.y() * 64) + ((Math.abs(((int) ((y - Math.floor(y)) * 8)) - 8) - 1) * 8) + (int) ((z - Math.floor(z)) * 8)]) {
                        if (recordFriction) {
                            if (typeId == 7) { //kyanite
                                friction = Math.min(friction, 0.95f);
                            } else if (typeId == 11 || typeId == 12 || typeId == 13) { //glass
                                friction = Math.min(friction, 0.85f);
                            } else if (Tags.planks.tagged.contains(block.x)) { //wood
                                friction = Math.min(friction, 0.5f);
                            } else if (BlockTypes.blockTypeMap.get(typeId).blockProperties.isCollidable) {
                                friction = Math.min(friction, 0.75f);
                            }
                            blockOn = block;
                            setSolidBlockOn = true;
                        }
                        if (recordBounciness) {
                            if (typeId == 7) { //kyanite
                                bounciness = Math.min(bounciness, -2f);
                            } else if (typeId == 11 || typeId == 12 || typeId == 13) { //glass
                                bounciness = Math.min(bounciness, -0.33f);
                            }
                        }
                        return true;
                    }
                }
            }
            if (!setSolidBlockOn && recordFriction) {
                blockOn = block;
            }
        }
        return false;
    }
    public boolean solid(float x, float y, float z, float w, float h, boolean recordFriction, boolean recordBounciness) {
        setSolidBlockOn = false;
        boolean returnValue = false;
        for (float newX = x-w; newX <= x+w; newX+=0.125f) {
            for (float newY = y; newY <= y + h; newY += 0.125f) {
                for (float newZ = z - w; newZ <= z + w; newZ += 0.125f) {
                    if (solid(newX, newY, newZ, recordFriction, recordBounciness)) {
                        if (recordFriction) {
                            returnValue = true;
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
        return returnValue;
    }

    public void tick() {
        if (!Renderer.worldChanged) {
            new Matrix4f(camera.getViewMatrix()).getTranslation(oldCamOffset);
            blockBreathing = World.getBlock(blockPos.x, blockPos.y+eyeHeight, blockPos.z);
            speed = baseSpeed;
            boolean hittingCeiling = solid(pos.x, pos.y+height, pos.z, width, 0.125f, false, false);
            boolean mightBeCrawling = false;
            if (!crouching && !hittingCeiling) {
                height = Math.min(height+0.125f, baseHeight);
                eyeHeight = Math.min(eyeHeight+0.125f, baseEyeHeight);
            } else {
                mightBeCrawling = true;
            }
            if (crouching || mightBeCrawling) {
                if (sprint) {
                    crawling = true;
                } else {
                    crawling = false;
                }
                if (!crawling) {
                    if (height < baseHeight * 0.83f) {
                        speed = baseSpeed*0.5f;
                        if (!hittingCeiling) {
                            height = Math.min(height + 0.125f, baseHeight * 0.83f);
                            eyeHeight = Math.min(eyeHeight + 0.125f, baseEyeHeight * 0.83f);
                        }
                    } else {
                        speed = baseSpeed * 0.75f;
                        if (crouching) {
                            height = Math.max(height - 0.125f, baseHeight * 0.83f);
                            eyeHeight = Math.max(eyeHeight - 0.125f, baseEyeHeight * 0.83f);
                        }
                    }
                } else {
                    speed = baseSpeed*0.5f;
                    height = Math.max(height-0.125f, baseHeight*0.5f);
                    eyeHeight = Math.max(eyeHeight-0.125f, baseEyeHeight*0.5f);
                }

                sprint = false;
                superSprint = false;
            }
            camera.viewMatrix.setTranslation(new Vector3f(0, Player.eyeHeight, 0));
            if (!creative) {
                flying = false;
            }
            if (!musicSource.isPlaying()) {
                try {
                    SFX sfx = AudioController.loadRandomSound("/music/");
                    if (sfx != null) {
                        musicSource.play(sfx);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            friction = 1f;
            boolean onGround = solid(pos.x, pos.y-0.125f, pos.z, width, 0.125f, true, false);
            blockIn = World.getBlock(blockPos.x, blockPos.y, blockPos.z);
            submerged = BlockTypes.blockTypeMap.get(blockBreathing.x).blockProperties.isFluid;
            if (submerged) {
                AL10.alListenerf(AL10.AL_GAIN, 0.2f);
            }
            if (blockIn.x == 0 && onGround && sprint) {
                Vector2i blocKBelow = World.getBlock(blockPos.x, blockPos.y-1, blockPos.z);
                if (blocKBelow.x == BlockTypes.getId(BlockTypes.GRASS)) {
                    World.setBlock(blockPos.x, blockPos.y-1, blockPos.z, BlockTypes.getId(BlockTypes.DIRT), 0, true, false, 1, false);
                }
            }
            float modifiedSpeed = speed;
            float modifiedGrav = grav;
            if (blockIn.x == 1) { //water
                modifiedGrav *= 0.2f;
                friction *= 0.9f;
                modifiedSpeed *= 0.5f;
            } else if (Tags.leaves.tagged.contains(blockIn.x)) { //leaves
                if (blockIn.y == 0) {
                    friction *= 0.5f;
                    modifiedSpeed *= 0.5f;
                } else {
                    friction *= 0.9f;
                    modifiedSpeed *= 0.9f;
                }
            }
            friction = Math.min(0.99f, friction); //1-airFriction=maxFriction
            if (flying) {
                modifiedSpeed = speed;
                modifiedGrav = grav;
                friction = 0.75f;
            }

            Vector3f newMovement = new Vector3f(0f);
            boolean canMove = (flying || onGround || blockIn.x == 1);
            if (forward || backward) {
                Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, 0, (modifiedSpeed * (canMove ? 1 : 0.1f)) * (sprint || superSprint ? (backward ? (superSprint && sprint ? 100 : (superSprint ? 10 : 1)) : (flying ? (superSprint ? 100 : 10) : 2)) : 1) * (forward ? -1 : 1)).getTranslation(new Vector3f());
                newMovement.add(pos.x - translatedPos.x,0, pos.z - translatedPos.z);
            }
            if (rightward || leftward) {
                Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate((modifiedSpeed * (canMove ? 1 : 0.1f)) * (sprint || superSprint ? (flying ? (superSprint ? 100 : 10) : 2) : 1) * (rightward ? -1 : 1), 0, 0).getTranslation(new Vector3f());
                newMovement.add(pos.x - translatedPos.x, 0, pos.z - translatedPos.z);
            }
            if (upward || downward) {
                if (flying) {
                    Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, speed * (downward ? (-10 * (sprint || superSprint ? (superSprint ? -5 : 0) : 1f)) : -12 * (sprint || superSprint ? (superSprint ? 20 : 2) : 1)), 0).getTranslation(new Vector3f());
                    newMovement.add(0, pos.y - translatedPos.y, 0);
                } else if (blockIn.x == 1 && submerged) {
                    Vector3f translatedPos = new Matrix4f(getCameraMatrixWithoutPitch()).translate(0, speed * (downward ? -10 : -12), 0).getTranslation(new Vector3f());
                    newMovement.add(0, pos.y - translatedPos.y, 0);
                }
            }
            movement = new Vector3f(Utils.furthestFromZero(newMovement.x, movement.x*friction), Utils.furthestFromZero(newMovement.y, movement.y*friction), Utils.furthestFromZero(newMovement.z, movement.z*friction));
            vel = new Vector3f(vel.x*friction, vel.y*friction, vel.z*friction);

            if (!flying && vel.y >= -1+modifiedGrav && !onGround) {
                vel.set(vel.x, vel.y-modifiedGrav, vel.z);
            }

            if (Main.timeMS-jump < 100 && !flying) { //prevent jumping when space bar was pressed longer than 0.1s ago or when flying
                if ((onGround || (blockIn.x == 1 && solid(pos.x, pos.y, pos.z, width*1.125f, height, false, false))) && !submerged) {
                    jump = 1000;
                    lastJump = Main.timeMS;
                    vel.y = Math.max(vel.y, jumpStrength);
                    BlockSFX sfx = BlockTypes.blockTypeMap.get(blockOn.x).blockProperties.blockSFX;
                    jumpSource.setPos(new Vector3f(pos).sub(0, 0.1f, 0));
                    jumpSource.setVel(new Vector3f(vel.x+movement.x, Math.max(vel.y+movement.y, jumpStrength), vel.z+movement.z));
                    jumpSource.setGain((float) (sfx.stepGain+((sfx.stepGain*Math.random())/3)));
                    jumpSource.setPitch((float) (sfx.stepPitch+((sfx.stepPitch*Math.random())/3)), 0);
                    jumpSource.play(sfx.stepIds[(int) (Math.random() * sfx.stepIds.length)], true);
                }
            }

            vel = new Vector3f(Math.clamp(vel.x, -1, 1), Math.clamp(vel.y, -1, 1), Math.clamp(vel.z, -1, 1));
            float mX = vel.x + movement.x;
            float mY = vel.y + movement.y;
            float mZ = vel.z + movement.z;
            Vector3f hitPos = pos;
            Vector3f destPos = new Vector3f(mX+pos.x, mY+pos.y, mZ+pos.z);
            Float maxX = null;
            Float maxY = null;
            Float maxZ = null;
            float detail = 1+Math.max(Math.abs(mX*256f), Math.max(Math.abs(mY*256f), Math.abs(mZ*256f)));
            for (int i = 0; i <= detail; i++) {
                Vector3f rayPos = new Vector3f(maxX != null ? maxX : pos.x+((destPos.x-pos.x)*(i/detail)), maxY != null ? maxY : pos.y+((destPos.y-pos.y)*(i/detail)), maxZ != null ? maxZ : pos.z+((destPos.z-pos.z)*(i/detail)));
                if (crouching) {
                    if (!solid(rayPos.x, hitPos.y - 0.125f, hitPos.z, width, 0.125f, true, false)) {
                        maxX = hitPos.x;
                        rayPos.x = hitPos.x;
                    }
                    if (!solid(hitPos.x, hitPos.y - 0.125f, rayPos.z, width, 0.125f, true, false)) {
                        maxZ = hitPos.z;
                        rayPos.z = hitPos.z;
                    }
                }
                if (solid(rayPos.x, rayPos.y, rayPos.z, width, height, false, false)) {
                    if (maxX == null) {
                        bounciness = 0.66f;
                        if (solid(rayPos.x, hitPos.y, hitPos.z, width, height, false, !flying)) {
                            bounciness = Math.max(bounciness, -0.66f);
                            maxX = hitPos.x;
                            vel.x *= bounciness;
                            movement.x *= bounciness;
                        }
                    }
                    if (maxY == null) {
                        bounciness = 0.66f;
                        if (solid(hitPos.x, rayPos.y, hitPos.z, width, height, false, !flying)) {
                            bounciness = (upward && mY <= 0.f) ? bounciness : Math.max(bounciness, -0.66f); //dont limit bounciness if jumping and moving downwards
                            maxY = hitPos.y;
                            vel.y *= bounciness;
                            movement.y *= bounciness;
                        }
                    }
                    if (maxZ == null) {
                        bounciness = 0.66f;
                        if (solid(hitPos.x, hitPos.y, rayPos.z, width, height, false, !flying)) {
                            bounciness = Math.max(bounciness, -0.66f);
                            maxZ = hitPos.z;
                            vel.z *= bounciness;
                            movement.z *= bounciness;
                        }
                    }
                    if (maxX != null && maxY != null && maxZ != null) {
                        break;
                    }
                } else {
                    hitPos = rayPos;
                }
            }
            setPos(hitPos);
        }
    }

    public void setCameraMatrix(float[] matrix) {
        camera.setViewMatrix(matrix);
        camera.move(0, eyeHeight - camera.getViewMatrix().getTranslation(new Vector3f()).y(), 0, false);
    }
    public void setCameraPitch(Quaternionf pitch) {
        camera.setPitch(pitch);
    }
    public Matrix4f getCameraMatrix() {
        Vector3f camOffset = new Vector3f();
        Matrix4f camMatrix = new Matrix4f(camera.getViewMatrix());
        camMatrix.getTranslation(camOffset);
        camOffset = Utils.getInterpolatedVec(oldCamOffset, camOffset);
        Vector3f interpolatedPos = Utils.getInterpolatedVec(oldPos, pos);
        return camMatrix.setTranslation(interpolatedPos.x+camOffset.x, interpolatedPos.y+camOffset.y, interpolatedPos.z+camOffset.z);
    }
    public Matrix4f getCameraMatrixWithoutPitch() {
        Vector3f camOffset = new Vector3f();
        Matrix4f camMatrix = new Matrix4f(camera.getViewMatrixWithoutPitch());
        camMatrix.getTranslation(camOffset);
        return camMatrix.setTranslation(pos.x+camOffset.x, pos.y+camOffset.y, pos.z+camOffset.z);
    }

    boolean updatedWater = false;
    int ambientWater = 0;
    boolean updatedMagma = false;
    int ambientMagma = 0;
    int ambientWind = 0;
    public void playBlocksAmbientSound(int block, int sunLight) {
        Vector2i unpackedBlock = Utils.unpackInt(block);
        if (unpackedBlock.x == 1 && !updatedWater) {
            updatedWater = true;
            ambientWater = Math.min(333, ambientWater+33);
        } else if (unpackedBlock.x == 19 && !updatedMagma) {
            updatedMagma = true;
            ambientMagma = Math.min(333, ambientMagma+33);
        }

        if (Math.random() <= 0.0002d) {
            if (Tags.leaves.tagged.contains(unpackedBlock.x)) {
                Source source = new Source(pos, 0.01f*sunLight, 1, 1, 0);
                source.play(Sounds.CHIRP1);
            }
        } else if (Math.random() <= 0.0001d) {
            if (Tags.flowers.tagged.contains(unpackedBlock.x)) {
                Source source = new Source(pos, 0.5f, 1, 1, 0);
                source.play(Sounds.BUZZ);
            }
        }
    }

    public long timeSinceAmbientSoundAttempt = 0;
    public float windGain = 0f;
    public long timeLastFootstepSoundPlayed = 0;

    public void setPos(Vector3f newPos) {
        oldPos = pos;
        pos = newPos;
        blockPos = new Vector3i((int) newPos.x, (int) newPos.y, (int) newPos.z);
        if (World.worldGenerated) {
            Matrix4f camNoPitch = camera.getViewMatrixWithoutPitch();
            AudioController.setListenerData(newPos, vel, new float[]{camNoPitch.m02(), camNoPitch.m20(), camNoPitch.m33(), camera.pitch.x, camera.pitch.y, camera.pitch.z});
            musicSource.setPos(oldPos);
            Vector3f combinedVel = new Vector3f(vel.x + movement.x, vel.y + movement.y, vel.z + movement.z);
            musicSource.setVel(combinedVel);
            float maxVel = Math.max(Math.abs(combinedVel.x), Math.max(Math.abs(combinedVel.y), Math.abs(combinedVel.z)));
            if (Math.abs(combinedVel.x) > 0.01 || Math.abs(combinedVel.y) > 0.01 || Math.abs(combinedVel.z) > 0.01) {
                int block = World.getBlock(newPos.x, newPos.y, newPos.z).x;
                if (block == 1) {
                    if (swimSource.soundPlaying == -1) {
                        submergeSource.play(Sounds.SPLASH1);
                    }
                    swimSource.play(Sounds.SWIM1);
                } else {
                    if (swimSource.soundPlaying != -1) {
                        swimSource.stop();
                        splashSource.play(Sounds.SPLASH1);
                    }
                }
                if (blockOn.x > 0 && System.currentTimeMillis()-timeLastFootstepSoundPlayed > 1200-Math.min(1000, 4000*maxVel)) {
                    timeLastFootstepSoundPlayed = System.currentTimeMillis();
                    BlockSFX sfx = BlockTypes.blockTypeMap.get(blockOn.x).blockProperties.blockSFX;
                    Source newSource = new Source(new Vector3f(oldPos).sub(0, height, 0), (float) (sfx.stepGain+((sfx.stepGain*Math.random())/3)), (float) (sfx.stepPitch+((sfx.stepPitch*Math.random())/3)), 0, 0);
                    AudioController.disposableSources.add(newSource);
                    newSource.setVel(combinedVel);
                    newSource.play((sfx.stepIds[(int) (Math.random() * sfx.stepIds.length)]), true);
                }
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime-timeSinceAmbientSoundAttempt >= 1000) {
                timeSinceAmbientSoundAttempt = currentTime;
                if (windSource.soundPlaying == -1) {
                    windSource.play(Sounds.WIND);
                }
                if (World.inBounds(blockPos.x, blockPos.y, blockPos.z)) {
                    updatedWater = false;
                    int sunLight = World.getLight(blockPos).w;
                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                Vector3i chunkPos = new Vector3i((blockPos.x / 16) + x, (blockPos.y / 16) + y, (blockPos.z / 16) + z);
                                if (World.inBoundsChunk(chunkPos.x, chunkPos.y, chunkPos.z)) {
                                    for (int block : World.chunks[Utils.condenseChunkPos(chunkPos)].blockPalette.toArray(new int[0])) {
                                        playBlocksAmbientSound(block, sunLight);
                                    }
                                }
                            }
                        }
                    }
                    if (waterFlowingSource.soundPlaying == -1) {
                        waterFlowingSource.play(Sounds.FLOW);
                    }
                    waterFlowingSource.setGain(Math.clamp(ambientWater / 10000f, 0, 1));
                    if (magmaSource.soundPlaying == -1) {
                        magmaSource.play(Sounds.MAGMA);
                    }
                    magmaSource.setGain(Math.clamp(ambientMagma / 333f, 0, 1));
                    ambientWind = Math.min(333, ambientWind + sunLight);
                    windGain = ambientWind/333f;
                }
            }
            float velocity = (Math.max(Math.abs(vel.x+movement.x), Math.max(Math.abs(vel.y+movement.y), Math.abs(vel.z+movement.z))));
            windSource.setGain(Math.clamp(windGain+(Math.max(0.05f, velocity/10)-0.05f), 0, 1));
            ambientWind = Math.max(0, ambientWind-1);
            ambientWater = Math.max(0, ambientWater-1);
            waterFlow = Math.max(0, waterFlow-1);

            splashSource.setPos(oldPos);
            splashSource.setVel(combinedVel);
            submergeSource.setPos(oldPos);
            submergeSource.setVel(combinedVel);
            swimSource.baseGain = Math.min(0.5f, maxVel*100);
            swimSource.setPos(oldPos);
            swimSource.setVel(combinedVel);
            passthroughSource.setPos(oldPos);
            passthroughSource.setVel(combinedVel);
        }
    }

    public void move(float x, float y, float z, boolean countRotation) {
        if (countRotation) {
            Vector3f returnPos = new Vector3f();
            Matrix4f tempMatrix = new Matrix4f(camera.getViewMatrixWithoutPitch()).setTranslation(0, 0, 0).translate(x, y, z);
            tempMatrix.getTranslation(returnPos);
            setPos(new Vector3f(pos.x + returnPos.x, pos.y + returnPos.y, pos.z + returnPos.z));
        } else {
            setPos(new Vector3f(pos.x + x, pos.y + y, pos.z + z));
        }
    }

    public void rotate(float pitch, float yaw) {
        camera.rotate(pitch, yaw);
    }
}