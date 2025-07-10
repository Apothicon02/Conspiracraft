package org.conspiracraft;

import org.conspiracraft.engine.*;
import org.conspiracraft.game.Player;
import org.conspiracraft.game.World;
import org.conspiracraft.game.audio.AudioController;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL;

import static java.lang.System.out;
import static org.lwjgl.glfw.GLFW.*;


public class Main {
    boolean isClosing = false;
    public static Player player;
    private static final float MOUSE_SENSITIVITY = 0.01f;
    private static final float MOVEMENT_SPEED = 0.005f;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        Engine gameEng = new Engine("Conspiracraft", new Window.WindowOptions(), main);
        gameEng.start();
    }

    public void init(Window window) throws Exception {
        GL.createCapabilities();
        AudioController.init();
        AudioController.setListenerData(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new float[6]);
        AudioController.loadSound("jump.wav");
        AudioController.loadSound("grass_step1.wav");
        AudioController.loadSound("grass_step2.wav");
        AudioController.loadSound("grass_step3.wav");
        AudioController.loadSound("dirt_step1.wav");
        AudioController.loadSound("dirt_step2.wav");
        AudioController.loadSound("dirt_step3.wav");
        AudioController.loadSound("swim1.wav");
        AudioController.loadSound("splash1.wav");
        AudioController.loadSound("flow.wav");
        AudioController.loadSound("wind.wav");
        AudioController.loadSound("buzz.wav");
        AudioController.loadSound("chirp_1.wav");
        AudioController.loadSound("magma.wav");
        AudioController.loadRandomSound("Music/");
        Renderer.init(window);
        World.init();
        player = new Player(new Vector3f(8, 8, 8));
    }

    public static double interpolationTime = 0;
    public static double timePassed = 0;
    public static double tickTime = 25;
    public static float worldTickStage = 0f;
    public static long timeMS;

    public void update(Window window, long diffTimeMillis, long time) throws Exception {
        if (isClosing) {
            out.close();
            glfwSetWindowShouldClose(window.getWindowHandle(), true); // We will detect this in the rendering loop
        }
        timeMS = time;
        while (timePassed >= tickTime) {
            timePassed -= tickTime;
            worldTickStage+=0.025f;
            if (worldTickStage >= 0.976f) {
                worldTickStage = 0f;
            }
            World.tick(worldTickStage);
        }
        interpolationTime = timePassed/tickTime;
        timePassed += diffTimeMillis;
    }

    boolean wasXDown = false;
    boolean wasTDown = false;
    boolean wasGDown = false;
    boolean wasLDown = false;
    boolean wasUpDown = false;
    boolean wasDownDown = false;
    boolean wasEDown = false;
    boolean wasCDown = false;
    boolean wasWDown = false;
    boolean wasQDown = false;
    boolean wasF1Down = false;
    boolean wasF4Down = false;

    long lastBlockBroken = 0L;

    public void input(Window window, long timeMillis, long diffTimeMillis) {
        if (!isClosing) {
            if (window.isKeyPressed(GLFW_KEY_ESCAPE, GLFW_PRESS)) {
                isClosing = true;
            } else {
                window.getMouseInput().input(window);
                boolean isShiftDown = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT, GLFW_PRESS);
                player.sprint = isShiftDown;
                player.superSprint = window.isKeyPressed(GLFW_KEY_CAPS_LOCK, GLFW_PRESS);
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
//                if (timeMillis - lastBlockBroken >= 200) { //two tenth second minimum delay between breaking blocks
//                    boolean lmbDown = mouseInput.isLeftButtonPressed();
//                    boolean mmbDown = mouseInput.isMiddleButtonPressed();
//                    boolean rmbDown = mouseInput.isRightButtonPressed();
//                    if (lmbDown || mmbDown || rmbDown) {
//                        Vector3f pos = raycast(new Matrix4f(player.getCameraMatrix()), lmbDown || mmbDown, 100, mmbDown);
//                        if (pos != null) {
//                            if (mmbDown) {
//                                Vector2i block = World.getBlock(pos.x, pos.y, pos.z);
//                                if (block != null) {
//                                    selectedBlock = Handcrafting.interact(selectedBlock, block);
//                                }
//                            } else if (BlockTypes.blockTypeMap.get(selectedBlock.x()) != null) {
//                                lastBlockBroken = timeMillis;
//                                int blockTypeId = selectedBlock.x();
//                                int blockSubtypeId = selectedBlock.y();
//                                int amount = selectedBlock.z();
//                                int cornerData = World.getCorner((int) pos.x, (int) pos.y, (int) pos.z);
//                                int cornerIndex = (pos.y < (int)(pos.y)+0.5 ? 0 : 4) + (pos.z < (int)(pos.z)+0.5 ? 0 : 2) + (pos.x < (int)(pos.x)+0.5 ? 0 : 1);
//                                if (lmbDown) {
//                                    cornerData |= (1 << (cornerIndex - 1));
//                                    if (cornerData == -2147483521 || !isShiftDown) {
//                                        Vector2i blockBreaking = World.getBlock(pos.x, pos.y, pos.z);
//                                        if (amount == 0 || (blockTypeId == blockBreaking.x && amount < 16)) {
//                                            selectedBlock = new Vector3i(blockBreaking, amount+1);
//                                            World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, 0);
//                                            blockTypeId = 0;
//                                            blockSubtypeId = 0;
//                                            World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockTypeId, blockSubtypeId, true, false);
//                                        }
//                                    } else {
//                                        World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, cornerData);
//                                    }
//                                }
//                                if (rmbDown) {
//                                    if (cornerData != 0) {
//                                        cornerData &= (~(1 << (cornerIndex - 1)));
//                                        World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, cornerData);
//                                    } else if (amount > 0) {
//                                        World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockTypeId, blockSubtypeId, true, false);
//                                        selectedBlock.z--;
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//
//                if (wasF1Down && !window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS)) {
//                    Renderer.showUI = !Renderer.showUI;
//                }
//                if (wasF4Down && !window.isKeyPressed(GLFW_KEY_F4, GLFW_PRESS)) {
//                    Renderer.shadowsEnabled = !Renderer.shadowsEnabled;
//                }
//
//                if (window.isKeyPressed(GLFW_KEY_F3, GLFW_PRESS)) {
//                    if (wasCDown && !window.isKeyPressed(GLFW_KEY_C, GLFW_PRESS)) {
//                        Renderer.cloudsEnabled = !Renderer.cloudsEnabled;
//                    }
//                    if (wasEDown && !window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS)) {
//                        selectedBlock.add(new Vector3i(0, isShiftDown ? 100 : 1, selectedBlock.z()));
//                    } else if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
//                        int newSubId = selectedBlock.y() - (isShiftDown ? 100 : 1);
//                        if (newSubId < 0) {
//                            newSubId = 0;
//                        }
//                        selectedBlock = new Vector3i(selectedBlock.x, newSubId, selectedBlock.z());
//                    }
//                    if (wasWDown && !window.isKeyPressed(GLFW_KEY_W, GLFW_PRESS)) {
//                        Renderer.snowing = !Renderer.snowing;
//                    }
//                    if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
//                        Renderer.worldChanged = true;
//                    }
//                    if (wasUpDown && !window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS)) {
//                        if (Renderer.renderDistanceMul < 200) {
//                            Renderer.renderDistanceMul++;
//                        }
//                    }
//                    if (wasDownDown && !window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS)) {
//                        if (Renderer.renderDistanceMul > 0) {
//                            Renderer.renderDistanceMul--;
//                        }
//                    }
//                } else {
//                    if (wasEDown && !window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS)) {
//                        int newId = selectedBlock.x() + 1;
//                        if (newId >= BlockTypes.blockTypeMap.size()) {
//                            newId = 0;
//                        }
//                        selectedBlock = new Vector3i(newId, selectedBlock.y, selectedBlock.z());
//                    } else if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
//                        int newId = selectedBlock.x() - 1;
//                        if (newId < 0) {
//                            newId = BlockTypes.blockTypeMap.size() - 1;
//                        }
//                        selectedBlock = new Vector3i(newId, selectedBlock.y, selectedBlock.z());
//                    }
//                    if (wasXDown && !window.isKeyPressed(GLFW_KEY_X, GLFW_PRESS)) {
//                        player.flying = !player.flying;
//                    }
//                    if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
//                        updateTime(100000L, 1);
//                    }
//                    if (wasUpDown && !window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS)) {
//                        selectedBlock.z++;
//                        if (selectedBlock.z > 16) {
//                            selectedBlock.z = 16;
//                        }
//                    }
//                    if (wasDownDown && !window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS)) {
//                        selectedBlock.z--;
//                        if (selectedBlock.z < 0) {
//                            selectedBlock.z = 0;
//                        }
//                    }
//                }

                wasF1Down = window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS);
                wasF4Down = window.isKeyPressed(GLFW_KEY_F4, GLFW_PRESS);
                wasQDown = window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS);
                wasEDown = window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS);
                wasCDown = window.isKeyPressed(GLFW_KEY_C, GLFW_PRESS);
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
}
