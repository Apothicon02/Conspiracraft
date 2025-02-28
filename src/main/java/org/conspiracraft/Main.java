package org.conspiracraft;

import org.conspiracraft.game.Noise;
import org.conspiracraft.game.Player;
import org.conspiracraft.game.audio.AudioController;
import org.conspiracraft.game.rendering.Renderer;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.Chunk;
import org.conspiracraft.game.world.World;
import org.joml.*;
import org.conspiracraft.engine.*;
import org.lwjgl.opengl.GL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;

public class Main {
    public static Player player;
    private static final float MOUSE_SENSITIVITY = 0.01f;
    private static final float MOVEMENT_SPEED = 0.005f;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        Engine gameEng = new Engine("Conspiracraft", new Window.WindowOptions(), main);
        gameEng.start();
    }

    public void init(Window window) throws Exception {
        Noise.init();
        GL.createCapabilities();
        AudioController.init();
        AudioController.setListenerData(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0));
        AudioController.loadSound("jump.wav");
        AudioController.loadSound("grass_step1.wav");
        AudioController.loadSound("grass_step2.wav");
        AudioController.loadSound("grass_step3.wav");
        AudioController.loadSound("dirt_step1.wav");
        AudioController.loadSound("dirt_step2.wav");
        AudioController.loadSound("dirt_step3.wav");
        AudioController.loadSound("swim1.wav");
        AudioController.loadSound("splash1.wav");
        AudioController.loadRandomSound("Music/");
        String playerPath = (World.worldPath + "/local.player");
        if (Files.exists(Path.of(playerPath))) {
            FileInputStream in = new FileInputStream(playerPath);
            int[] data = Utils.flipIntArray(Utils.byteArrayToIntArray(in.readAllBytes()));
            float[] camMatrix = new float[16];
            int i = 0;
            while (i < 16) {
                camMatrix[i] = data[i]/1000f;
                i++;
            }
            Quaternionf pitch = new Quaternionf(data[i++]/1000f, data[i++]/1000f, data[i++]/1000f, data[i++]/1000f);
            Vector3f pos = new Vector3f(data[i++]/1000f, data[i++]/1000f, data[i++]/1000f);
            Vector3f vel = new Vector3f(data[i++]/1000f, data[i++]/1000f, data[i++]/1000f);
            player = new Player(pos);
            player.vel = vel;
            player.setCameraMatrix(camMatrix);
            player.setCameraPitch(pitch);
            player.flying = data[i++] != 0;
        } else {
            player = new Player(new Vector3f(512, 256, 512));
        }
        String globalPath = (World.worldPath + "/global.world");
        if (Files.exists(Path.of(globalPath))) {
            FileInputStream in = new FileInputStream(globalPath);
            int[] data = Utils.flipIntArray(Utils.byteArrayToIntArray(in.readAllBytes()));
            Renderer.time = data[0]/1000f;
            timePassed = data[1]/1000f;
            meridiem = data[2];
        }
        String heightmapPath = (World.worldPath + "/global.heightmap");
        if (Files.exists(Path.of(heightmapPath))) {
            FileInputStream in = new FileInputStream(heightmapPath);
            World.heightmap = Utils.intArrayToShortArray(Utils.byteArrayToIntArray(in.readAllBytes()));
        }
    }

    boolean wasXDown = false;
    boolean wasTDown = false;
    boolean wasGDown = false;
    boolean wasLDown = false;
    boolean wasUpDown = false;
    boolean wasDownDown = false;
    boolean wasEDown = false;
    boolean wasWDown = false;
    boolean wasQDown = false;
    boolean wasF1Down = false;
    boolean wasF4Down = false;
    boolean isClosing = false;

    long lastBlockBroken = 0L;
    Vector2i selectedBlock = new Vector2i(15, 0);

    public void input(Window window, long timeMillis, long diffTimeMillis) {
        if (!isClosing) {
            if (window.isKeyPressed(GLFW_KEY_ESCAPE, GLFW_PRESS)) {
                isClosing = true;
            } else {
                window.getMouseInput().input(window);
                boolean isShiftDown = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT, GLFW_PRESS);
                player.sprint = isShiftDown;
                player.forward = window.isKeyPressed(GLFW_KEY_W, GLFW_PRESS);
                player.backward = window.isKeyPressed(GLFW_KEY_S, GLFW_PRESS);
                player.rightward = window.isKeyPressed(GLFW_KEY_D, GLFW_PRESS);
                player.leftward = window.isKeyPressed(GLFW_KEY_A, GLFW_PRESS);
                player.upward = window.isKeyPressed(GLFW_KEY_SPACE, GLFW_PRESS);
                player.downward = window.isKeyPressed(GLFW_KEY_LEFT_CONTROL, GLFW_PRESS);

                if (window.isKeyPressed(GLFW_KEY_SPACE, GLFW_PRESS) && timeMillis - player.lastJump > 200) { //only jump at most five times a second
                    player.jump = timeMillis;
                }

                MouseInput mouseInput = window.getMouseInput();
                Vector2f displVec = mouseInput.getDisplVec();
                player.rotate((float) Math.toRadians(displVec.x * MOUSE_SENSITIVITY),
                        (float) Math.toRadians(displVec.y * MOUSE_SENSITIVITY));
                if (timeMillis - lastBlockBroken >= 200) { //two tenth second minimum delay between breaking blocks
                    boolean lmbDown = mouseInput.isLeftButtonPressed();
                    boolean mmbDown = mouseInput.isMiddleButtonPressed();
                    boolean rmbDown = mouseInput.isRightButtonPressed();
                    if (lmbDown || mmbDown || rmbDown) {
                        Vector3f pos = raycast(new Matrix4f(player.getCameraMatrix()), lmbDown || mmbDown, 100, mmbDown);
                        if (pos != null) {
                            if (mmbDown) {
                                selectedBlock = World.getBlock((int) pos.x, (int) pos.y, (int) pos.z);
                            } else if (BlockTypes.blockTypeMap.get(selectedBlock.x()) != null) {
                                lastBlockBroken = timeMillis;
                                int blockTypeId = selectedBlock.x();
                                int blockSubtypeId = selectedBlock.y();
                                int cornerData = World.getCorner((int) pos.x, (int) pos.y, (int) pos.z);
                                int cornerIndex = (pos.y < (int)(pos.y)+0.5 ? 0 : 4) + (pos.z < (int)(pos.z)+0.5 ? 0 : 2) + (pos.x < (int)(pos.x)+0.5 ? 0 : 1);
                                if (lmbDown) {
                                    cornerData |= (1 << (cornerIndex - 1));
                                    World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, cornerData);
                                    if (cornerData == -2147483521 || !isShiftDown) {
                                        World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, 0);
                                        blockTypeId = 0;
                                        blockSubtypeId = 0;
                                        World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockTypeId, blockSubtypeId, true, false);
                                    }
                                }
                                if (rmbDown) {
                                    if (cornerData != 0) {
                                        cornerData &= (~(1 << (cornerIndex - 1)));
                                        World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, cornerData);
                                    } else {
                                        World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockTypeId, blockSubtypeId, true, false);
                                    }
                                }
                            }
                        }
                    }
                }

                if (wasF1Down && !window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS)) {
                    Renderer.showUI = !Renderer.showUI;
                }
                if (wasF4Down && !window.isKeyPressed(GLFW_KEY_F4, GLFW_PRESS)) {
                    Renderer.shadowsEnabled = !Renderer.shadowsEnabled;
                }

                if (window.isKeyPressed(GLFW_KEY_F3, GLFW_PRESS)) {
                    if (wasEDown && !window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS)) {
                        selectedBlock.add(new Vector2i(0, isShiftDown ? 100 : 1));
                    } else if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
                        int newSubId = selectedBlock.y() - (isShiftDown ? 100 : 1);
                        if (newSubId < 0) {
                            newSubId = 0;
                        }
                        selectedBlock = new Vector2i(selectedBlock.x, newSubId);
                    }
                    if (wasWDown && !window.isKeyPressed(GLFW_KEY_W, GLFW_PRESS)) {
                        Renderer.snowing = !Renderer.snowing;
                    }
                    if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
                        Renderer.worldChanged = true;
                    }
                    if (wasUpDown && !window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS)) {
                        if (Renderer.renderDistanceMul < 200) {
                            Renderer.renderDistanceMul++;
                        }
                    }
                    if (wasDownDown && !window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS)) {
                        if (Renderer.renderDistanceMul > 0) {
                            Renderer.renderDistanceMul--;
                        }
                    }
                } else {
                    if (wasEDown && !window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS)) {
                        int newId = selectedBlock.x() + 1;
                        if (newId >= BlockTypes.blockTypeMap.size()) {
                            newId = 0;
                        }
                        selectedBlock = new Vector2i(newId, selectedBlock.y);
                    } else if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
                        int newId = selectedBlock.x() - 1;
                        if (newId < 0) {
                            newId = BlockTypes.blockTypeMap.size() - 1;
                        }
                        selectedBlock = new Vector2i(newId, selectedBlock.y);
                    }
                    if (wasXDown && !window.isKeyPressed(GLFW_KEY_X, GLFW_PRESS)) {
                        player.flying = !player.flying;
                    }
                    if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
                        updateTime(100000L, 1);
                    }
                }

                wasF1Down = window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS);
                wasF4Down = window.isKeyPressed(GLFW_KEY_F4, GLFW_PRESS);
                wasQDown = window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS);
                wasEDown = window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS);
                wasWDown = window.isKeyPressed(GLFW_KEY_W, GLFW_PRESS);
                wasTDown = window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS);
                wasXDown = window.isKeyPressed(GLFW_KEY_X, GLFW_PRESS);
                wasGDown = window.isKeyPressed(GLFW_KEY_G, GLFW_PRESS);
                wasLDown = window.isKeyPressed(GLFW_KEY_L, GLFW_PRESS);
                wasUpDown = window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS);
                wasDownDown = window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS);
            }
        }
    }

    public static int meridiem = 1;

    public void updateTime(long diffTimeMillis, float mul) {
        Renderer.time += (diffTimeMillis/600000f)*mul;
        float time = Renderer.timeOfDay+((diffTimeMillis/600000f)*mul)*meridiem;
        if (time < 0f) {
            time = 0;
            meridiem = 1;
        } else if (time > 1f) {
            time = 1;
            meridiem = -1;
        }
        Renderer.timeOfDay = time;
    }

    public static boolean postWorldgenInitialization = false;
    public static double interpolationTime = 0;
    public static double timePassed = 0;
    public static double tickTime = 50;
    public static long timeMS;

    public void update(Window window, long diffTimeMillis, long time) throws Exception {
        timeMS = time;
        if (isClosing) {
            String path = (World.worldPath+"/");
            new File(path).mkdirs();

            String playerDataPath = path+"local.player";
            FileOutputStream out = new FileOutputStream(playerDataPath);
            byte[] playerData = Utils.intArrayToByteArray(player.getData());
            out.write(playerData);
            out.close();

            String globalDataPath = path+"global.world";
            out = new FileOutputStream(globalDataPath);
            byte[] globalData = Utils.intArrayToByteArray(new int[]{(int)(Renderer.time*1000), (int)(timePassed*1000), meridiem});
            out.write(globalData);
            out.close();

            String heightmapDataPath = path+"global.heightmap";
            out = new FileOutputStream(heightmapDataPath);
            byte[] heightmapData = Utils.intArrayToByteArray(Utils.shortArrayToIntArray(World.heightmap));
            out.write(heightmapData);
            out.close();

            String chunksPath = path + "chunks/";
            new File(chunksPath).mkdirs();
            for (int x = 0; x < World.sizeChunks; x++) {
                for (int z = 0; z < World.sizeChunks; z++) {
                    int columnPos = Utils.condenseChunkPos(x, z);
                    if (World.columnUpdates[columnPos]) {
                        World.columnUpdates[columnPos] = false;
                        String chunkPath = chunksPath + x + "x" + z + "z.column";
                        out = new FileOutputStream(chunkPath);
                        for (int y = 0; y < World.heightChunks; y++) {
                            Chunk chunk = World.chunks[Utils.condenseChunkPos(x, y, z)];
                            byte[] subChunks = Utils.intArrayToByteArray(chunk.getSubChunks());

                            byte[] blockPalette = Utils.intArrayToByteArray(chunk.getBlockPalette());
                            int[] blockData = chunk.getBlockData();
                            byte[] blocks;
                            if (blockData != null) {
                                blocks = Utils.intArrayToByteArray(blockData);
                            } else {
                                blocks = new byte[]{};
                            }

                            byte[] cornerPalette = Utils.intArrayToByteArray(chunk.getCornerPalette());
                            int[] cornerData = chunk.getCornerData();
                            byte[] corners;
                            if (cornerData != null) {
                                corners = Utils.intArrayToByteArray(cornerData);
                            } else {
                                corners = new byte[]{};
                            }

                            byte[] lightPalette = Utils.intArrayToByteArray(chunk.getLightPalette());
                            int[] lightData = chunk.getLightData();
                            byte[] lights;
                            if (lightData != null) {
                                lights = Utils.intArrayToByteArray(lightData);
                            } else {
                                lights = new byte[]{};
                            }
                            ByteBuffer buffer = ByteBuffer.allocate(subChunks.length + 4 + blockPalette.length + 4 + blocks.length + 4 + cornerPalette.length + 4 + corners.length + 4 + lightPalette.length + 4 + lights.length + 4);
                            buffer.put(Utils.intArrayToByteArray(new int[]{subChunks.length / 4}));
                            buffer.put(subChunks);
                            buffer.put(Utils.intArrayToByteArray(new int[]{blockPalette.length / 4}));
                            buffer.put(blockPalette);
                            buffer.put(Utils.intArrayToByteArray(new int[]{blocks.length / 4}));
                            buffer.put(blocks);
                            buffer.put(Utils.intArrayToByteArray(new int[]{cornerPalette.length / 4}));
                            buffer.put(cornerPalette);
                            buffer.put(Utils.intArrayToByteArray(new int[]{corners.length / 4}));
                            buffer.put(corners);
                            buffer.put(Utils.intArrayToByteArray(new int[]{lightPalette.length / 4}));
                            buffer.put(lightPalette);
                            buffer.put(Utils.intArrayToByteArray(new int[]{lights.length / 4}));
                            buffer.put(lights);
                            out.write(buffer.array());
                        }
                        out.close();
                    }
                }
            }
            glfwSetWindowShouldClose(window.getWindowHandle(), true); // We will detect this in the rendering loop
        } else {
            World.run();
            if (World.worldGenerated) {
                if (!postWorldgenInitialization) {
                    postWorldgenInitialization = true;
                    Renderer.init(window);
                }
                updateTime(diffTimeMillis, 1);
                while (timePassed >= tickTime) {
                    timePassed -= tickTime;
                    player.tick();
                }
                interpolationTime = timePassed/tickTime;
                timePassed += diffTimeMillis;
            }
        }
    }

    public static Vector3f raycast(Matrix4f ray, boolean prevPos, int range, boolean countFluids) { //prevPos is inverted
        Vector3f prevRayPos = new Vector3f(ray.m30(), ray.m31(), ray.m32());
        for (int i = 0; i < range; i++) {
            Vector3f rayPos = new Vector3f(ray.m30(), ray.m31(), ray.m32());
            Vector2i block = World.getBlock(rayPos.x, rayPos.y, rayPos.z);
            if (block != null) {
                int typeId = block.x();
                int subTypeId = block.y();
                boolean isFluid = BlockTypes.blockTypeMap.get(typeId).isFluid;
                if (countFluids || !isFluid) {
                    if (isFluid) {
                        subTypeId = Math.min(20, subTypeId);
                    }
                    int cornerData = World.getCorner((int) rayPos.x, (int) rayPos.y, (int) rayPos.z);
                    int cornerIndex = (rayPos.y < (int) (rayPos.y) + 0.5 ? 0 : 4) + (rayPos.z < (int) (rayPos.z) + 0.5 ? 0 : 2) + (rayPos.x < (int) (rayPos.x) + 0.5 ? 0 : 1);
                    if (((cornerData & (1 << (cornerIndex - 1))) >> (cornerIndex - 1)) == 0) {
                        if (Renderer.collisionData[(9984 * ((typeId * 8) + (int) ((rayPos.x - Math.floor(rayPos.x)) * 8))) + (subTypeId * 64) + ((Math.abs(((int) ((rayPos.y - Math.floor(rayPos.y)) * 8)) - 8) - 1) * 8) + (int) ((rayPos.z - Math.floor(rayPos.z)) * 8)]) {
                            if (prevPos) {
                                return rayPos;
                            } else {
                                return prevRayPos;
                            }
                        }
                    }
                }
            }
            prevRayPos = rayPos;
            ray.translate(0, 0, 0.1f);
        }
        return null;
    }
}
