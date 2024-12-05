package org.conspiracraft.game;

import org.conspiracraft.game.blocks.Block;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.World;
import org.joml.*;
import org.conspiracraft.engine.*;

import java.lang.Math;
import static org.lwjgl.glfw.GLFW.*;

public class Main {
    public static Player player = new Player(new Vector3f(100, 150, 100));
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
        Renderer.init();
    }

    boolean wasTDown = false;
    boolean wasGDown = false;
    boolean wasLDown = false;
    boolean wasUpDown = false;
    boolean wasDownDown = false;

    long lastBlockBroken = 0L;
    Block selectedBlock = new Block(2, 0);

    public void input(Window window, long timeMillis, long diffTimeMillis) {
        window.getMouseInput().input(window);
        float move = diffTimeMillis * MOVEMENT_SPEED;
        if (window.isKeyPressed(GLFW_KEY_LEFT_SHIFT, GLFW_PRESS)) {
            move*=10;
        }
        if (window.isKeyPressed(GLFW_KEY_CAPS_LOCK, GLFW_PRESS)) {
            move*=100;
        }
        if (window.isKeyPressed(GLFW_KEY_W, GLFW_PRESS)) {
            player.move(0, 0, move, true);
        } else if (window.isKeyPressed(GLFW_KEY_S, GLFW_PRESS)) {
            player.move(0, 0, -move, true);
        }
        if (window.isKeyPressed(GLFW_KEY_A, GLFW_PRESS)) {
            player.move(-move, 0, 0, true);
        } else if (window.isKeyPressed(GLFW_KEY_D, GLFW_PRESS)) {
            player.move(move, 0, 0, true);
        }
        if (window.isKeyPressed(GLFW_KEY_SPACE, GLFW_PRESS)) {
            player.move(0, move, 0, false);
        } else if (window.isKeyPressed(GLFW_KEY_LEFT_CONTROL, GLFW_PRESS)) {
            player.move(0, -move, 0, false);
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
                Vector3f pos = raycast(new Matrix4f(player.getCameraMatrix()), lmbDown || mmbDown, 100);
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

        if (window.isKeyPressed(GLFW_KEY_F3, GLFW_PRESS)) {
            if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
                Renderer.atlasChanged = true;
            }
            if (wasGDown && !window.isKeyPressed(GLFW_KEY_G, GLFW_PRESS)) {
                World.regenerateWorld();
            }
            if (wasLDown && !window.isKeyPressed(GLFW_KEY_L, GLFW_PRESS)) {
                Renderer.worldChanged = true;
            }
            if (wasUpDown && !window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS)) {
                if (Renderer.renderDistanceMul < 4) {
                    Renderer.renderDistanceMul++;
                }
            }
            if (wasDownDown && !window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS)) {
                if (Renderer.renderDistanceMul > 0) {
                    Renderer.renderDistanceMul--;
                }
            }
        } else {
            if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
                updateTime(100000L, 1);
            }
        }

        wasTDown = window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS);
        wasGDown = window.isKeyPressed(GLFW_KEY_G, GLFW_PRESS);
        wasLDown = window.isKeyPressed(GLFW_KEY_L, GLFW_PRESS);
        wasUpDown = window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS);
        wasDownDown = window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS);
    }

    byte meridiem = 1;

    public void updateTime(long diffTimeMillis, float mul) {
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

    public void update(Window window, long diffTimeMillis) {
        updateTime(diffTimeMillis, 1);
        player.tick();
    }

    public Vector3f raycast(Matrix4f ray, boolean prevPos, int range) {
        Vector3f blockPos = null;
        for (int i = 0; i < range; i++) {
            ray.translate(0, 0, 0.1f);
            Vector3f rayPos = new Vector3f(ray.m30(), ray.m31(), ray.m32());
            Block block = World.getBlock(rayPos.x, rayPos.y, rayPos.z);
            if (block != null) {
                int typeId = block.typeId();
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
        return null;
    }
}
