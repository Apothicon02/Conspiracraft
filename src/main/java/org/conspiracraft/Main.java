package org.conspiracraft;

import org.conspiracraft.game.Noise;
import org.conspiracraft.game.Player;
import org.conspiracraft.game.rendering.Renderer;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.game.world.Chunk;
import org.conspiracraft.game.world.World;
import org.joml.*;
import org.conspiracraft.engine.*;
import org.lwjgl.opengl.GL;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.Math;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.*;

public class Main {
    public static Player player = new Player(new Vector3f(256, 310, 256));
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
    }

    boolean wasXDown = false;
    boolean wasTDown = false;
    boolean wasGDown = false;
    boolean wasLDown = false;
    boolean wasUpDown = false;
    boolean wasDownDown = false;
    boolean wasEDown = false;
    boolean wasQDown = false;
    boolean wasF1Down = false;
    boolean isClosing = false;

    long lastBlockBroken = 0L;
    Vector2i selectedBlock = new Vector2i(15, 0);

    public void input(Window window, long timeMillis, long diffTimeMillis) {
        if (!isClosing) {
            if (window.isKeyPressed(GLFW_KEY_ESCAPE, GLFW_PRESS)) {
                isClosing = true;
            } else {
                window.getMouseInput().input(window);
                player.sprint = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT, GLFW_PRESS);
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
                        Vector3f pos = raycast(new Matrix4f(player.getCameraMatrix()), lmbDown || mmbDown, 100, true);
                        if (pos != null) {
                            if (mmbDown) {
                                selectedBlock = World.getBlock((int) pos.x, (int) pos.y, (int) pos.z);
                            } else if (BlockTypes.blockTypeMap.get(selectedBlock.x()) != null) {
                                lastBlockBroken = timeMillis;
                                int blockTypeId = selectedBlock.x();
                                int blockSubtypeId = selectedBlock.y();
                                if (lmbDown) {
                                    blockTypeId = 0;
                                    blockSubtypeId = 0;
                                }
                                World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockTypeId, blockSubtypeId, true, false);
                                World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, 100);
                            }
                        }
                    }
                }

                if (wasF1Down && !window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS)) {
                    Renderer.showUI = !Renderer.showUI;
                }

                if (window.isKeyPressed(GLFW_KEY_F3, GLFW_PRESS)) {
                    if (wasEDown && !window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS)) {
                        selectedBlock.add(new Vector2i(0, 1));
                    } else if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
                        int newSubId = selectedBlock.y() - 1;
                        if (newSubId < 0) {
                            newSubId = 0;
                        }
                        selectedBlock = new Vector2i(selectedBlock.x, newSubId);
                    }
                    if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
                        Renderer.atlasChanged = true;
                    }
                    if (wasUpDown && !window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS)) {
                        if (Renderer.renderDistanceMul < 96) {
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
                wasQDown = window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS);
                wasEDown = window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS);
                wasTDown = window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS);
                wasXDown = window.isKeyPressed(GLFW_KEY_X, GLFW_PRESS);
                wasGDown = window.isKeyPressed(GLFW_KEY_G, GLFW_PRESS);
                wasLDown = window.isKeyPressed(GLFW_KEY_L, GLFW_PRESS);
                wasUpDown = window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS);
                wasDownDown = window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS);
            }
        }
    }

    byte meridiem = 1;

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
    public static double timePassed = 0;
    public static double tickTime = 50;

    public void update(Window window, long diffTimeMillis, long time) throws Exception {
        if (isClosing) {
            String path = (World.worldPath+"/");
            new File(path).mkdirs();
            for (int x = 0; x < World.sizeChunks; x++) {
                for (int z = 0; z < World.sizeChunks; z++) {
                    int columnPos = Utils.condenseChunkPos(x, z);
                    if (World.columnUpdates[columnPos]) {
                        World.columnUpdates[columnPos] = false;
                        String chunkPath = path + x + "x" + z + "z.column";
                        FileOutputStream out = new FileOutputStream(chunkPath);
                        for (int y = 0; y < World.heightChunks; y++) {
                            Chunk chunk = World.chunks[Utils.condenseChunkPos(x, y, z)];
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
                            ByteBuffer buffer = ByteBuffer.allocate(blockPalette.length + 4 + blocks.length + 4 + cornerPalette.length + 4 + corners.length + 4);
                            buffer.put(Utils.intArrayToByteArray(new int[]{blockPalette.length / 4}));
                            buffer.put(blockPalette);
                            buffer.put(Utils.intArrayToByteArray(new int[]{blocks.length / 4}));
                            buffer.put(blocks);
                            buffer.put(Utils.intArrayToByteArray(new int[]{cornerPalette.length / 4}));
                            buffer.put(cornerPalette);
                            buffer.put(Utils.intArrayToByteArray(new int[]{corners.length / 4}));
                            buffer.put(corners);
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
                    player.tick(time);
                }
                timePassed += diffTimeMillis;
            }
        }
    }

    public static Vector3f raycast(Matrix4f ray, boolean prevPos, int range, boolean countCollisionless) { //prevPos is inverted
        Vector3f blockPos = null;
        for (int i = 0; i < range; i++) {
            Vector3f rayPos = new Vector3f(ray.m30(), ray.m31(), ray.m32());
            Vector2i block = World.getBlock(rayPos.x, rayPos.y, rayPos.z);
            if (block != null) {
                int typeId = block.x();
                if (countCollisionless || BlockTypes.blockTypeMap.get(typeId).isCollidable) {
                    int subTypeId = block.y();
                    if (Renderer.collisionData[(9984 * ((typeId * 8) + (int) ((rayPos.x - Math.floor(rayPos.x)) * 8))) + (subTypeId * 64) + ((Math.abs(((int) ((rayPos.y - Math.floor(rayPos.y)) * 8)) - 8) - 1) * 8) + (int) ((rayPos.z - Math.floor(rayPos.z)) * 8)]) {
                        if (prevPos) {
                            return rayPos;
                        } else {
                            return blockPos;
                        }
                    } else {
                        blockPos = new Vector3f((float) Math.floor(rayPos.x), (float) Math.floor(rayPos.y), (float) Math.floor(rayPos.z));
                    }
                }
            }
            ray.translate(0, 0, 0.1f);
        }
        return null;
    }
}
