package org.conspiracraft;

import org.conspiracraft.game.Noise;
import org.conspiracraft.game.Player;
import org.conspiracraft.game.rendering.Renderer;
import org.conspiracraft.game.blocks.Block;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.game.world.Chunk;
import org.conspiracraft.game.world.World;
import org.joml.*;
import org.conspiracraft.engine.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL40;

import java.lang.Math;
import static org.lwjgl.glfw.GLFW.*;

public class Main {
    public static Player player = new Player(new Vector3f(World.size/2f, 320, World.size/2f));
    private static final float MOUSE_SENSITIVITY = 0.01f;
    private static final float MOVEMENT_SPEED = 0.005f;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        Engine gameEng = new Engine("Conspiracraft", new Window.WindowOptions(), main);
        gameEng.start();
    }

    public void init(Window window) throws Exception {
        Noise.init();
        World.init();
        GL.createCapabilities();
//        GL40.glEnable(GL40.GL_CULL_FACE);
//        GL40.glCullFace(GL40.GL_BACK);
    }

    boolean wasTDown = false;
    boolean wasGDown = false;
    boolean wasLDown = false;
    boolean wasUpDown = false;
    boolean wasDownDown = false;
    boolean wasEDown = false;
    boolean wasQDown = false;
    boolean wasF1Down = false;

    long lastBlockBroken = 0L;
    Block selectedBlock = new Block(2, 0);

    public void input(Window window, long timeMillis, long diffTimeMillis) {
        window.getMouseInput().input(window);
        player.sprint = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT, GLFW_PRESS);
        player.forward = window.isKeyPressed(GLFW_KEY_W, GLFW_PRESS);
        player.backward = window.isKeyPressed(GLFW_KEY_S, GLFW_PRESS);
        player.rightward = window.isKeyPressed(GLFW_KEY_D, GLFW_PRESS);
        player.leftward = window.isKeyPressed(GLFW_KEY_A, GLFW_PRESS);

        if (window.isKeyPressed(GLFW_KEY_SPACE, GLFW_PRESS) && timeMillis-player.lastJump > 200) { //only jump at most five times a second
            player.jump = timeMillis;
        }

        MouseInput mouseInput = window.getMouseInput();
        Vector2f displVec = mouseInput.getDisplVec();
        player.rotate((float) Math.toRadians(displVec.x * MOUSE_SENSITIVITY),
                (float) Math.toRadians(displVec.y * MOUSE_SENSITIVITY));
        if (timeMillis-lastBlockBroken >= 200) { //two tenth second minimum delay between breaking blocks
            boolean lmbDown = mouseInput.isLeftButtonPressed();
            boolean mmbDown = mouseInput.isMiddleButtonPressed();
            boolean rmbDown = mouseInput.isRightButtonPressed();
            if (lmbDown || mmbDown || rmbDown) {
                Vector3f pos = raycast(new Matrix4f(player.getCameraMatrix()), lmbDown || mmbDown, 100, true);
                if (pos != null) {
                    if (mmbDown) {
                        selectedBlock = World.getBlock((int) pos.x, (int) pos.y, (int) pos.z);
                    } else if (BlockTypes.blockTypeMap.get(selectedBlock.typeId()) != null) {
                        lastBlockBroken = timeMillis;
                        int blockTypeId = selectedBlock.typeId();
                        int blockSubtypeId = selectedBlock.subtypeId();
                        if (lmbDown) {
                            blockTypeId = 0;
                            blockSubtypeId = 0;
                        }
                        World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockTypeId, blockSubtypeId, true, false);
                    }
                }
            }
        }

        if (wasF1Down && !window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS)) {
            Renderer.showUI = !Renderer.showUI;
        }

        if (window.isKeyPressed(GLFW_KEY_F3, GLFW_PRESS)) {
            if (wasEDown && !window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS)) {
                int newSubId = selectedBlock.subtypeId()+1;
                selectedBlock = new Block(selectedBlock.typeId(), newSubId, selectedBlock.r(), selectedBlock.g(), selectedBlock.b(), selectedBlock.s());
            } else if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
                int newSubId = selectedBlock.subtypeId()-1;
                if (newSubId < 0) {
                    newSubId = 0;
                }
                selectedBlock = new Block(selectedBlock.typeId(), newSubId, selectedBlock.r(), selectedBlock.g(), selectedBlock.b(), selectedBlock.s());
            }
            if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
                Renderer.atlasChanged = true;
            }
            if (wasGDown && !window.isKeyPressed(GLFW_KEY_G, GLFW_PRESS)) {
                World.regenerateWorld();
            }
            if (wasLDown && !window.isKeyPressed(GLFW_KEY_L, GLFW_PRESS)) {
                Renderer.worldChanged = true;
                for (Chunk chunk : World.region1Chunks) {
                    chunk.cleanPalette();
                }
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
                int newId = selectedBlock.typeId()+1;
                if (newId >= BlockTypes.blockTypeMap.size()) {
                    newId = 0;
                }
                BlockType type = BlockTypes.blockTypeMap.get(newId);
                if (type instanceof LightBlockType lType) {
                    selectedBlock = new Block(newId, 0, lType.r, lType.g, lType.b, (byte) 0);
                } else {
                    selectedBlock = new Block(newId, 0);
                }
            } else if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
                int newId = selectedBlock.typeId()-1;
                if (newId < 0) {
                    newId = BlockTypes.blockTypeMap.size()-1;
                }
                BlockType type = BlockTypes.blockTypeMap.get(newId);
                if (type instanceof LightBlockType lType) {
                    selectedBlock = new Block(newId, 0, lType.r, lType.g, lType.b, (byte) 0);
                } else {
                    selectedBlock = new Block(newId, 0);
                }
            }
            if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
                updateTime(100000L, 1);
            }
        }

        wasF1Down = window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS);
        wasQDown = window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS);
        wasEDown = window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS);
        wasTDown = window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS);
        wasGDown = window.isKeyPressed(GLFW_KEY_G, GLFW_PRESS);
        wasLDown = window.isKeyPressed(GLFW_KEY_L, GLFW_PRESS);
        wasUpDown = window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS);
        wasDownDown = window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS);
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

    public static Vector3f raycast(Matrix4f ray, boolean prevPos, int range, boolean countCollisionless) { //prevPos is inverted
        Vector3f blockPos = null;
        for (int i = 0; i < range; i++) {
            Vector3f rayPos = new Vector3f(ray.m30(), ray.m31(), ray.m32());
            Block block = World.getBlock(rayPos.x, rayPos.y, rayPos.z);
            if (block != null) {
                int typeId = block.typeId();
                if (countCollisionless || BlockTypes.blockTypeMap.get(typeId).isCollidable) {
                    int subTypeId = block.subtypeId();
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
