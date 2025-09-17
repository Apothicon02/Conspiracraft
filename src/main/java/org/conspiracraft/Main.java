package org.conspiracraft;

import org.conspiracraft.game.Player;
import org.conspiracraft.game.Renderer;
import org.conspiracraft.game.World;
import org.joml.*;
import org.conspiracraft.engine.*;
import org.lwjgl.opengl.GL;

import java.lang.Math;

import static org.conspiracraft.game.Player.voxelColor;
import static org.lwjgl.glfw.GLFW.*;

public class Main {
    public static Player player = new Player(new Vector3f(0, 0, 0));
    private static final float MOUSE_SENSITIVITY = 0.01f;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        Engine gameEng = new Engine("Conspiracraft", new Window.WindowOptions(), main);
        gameEng.start();
    }

    public void init(Window window) throws Exception {
        GL.createCapabilities();
    }

    boolean wasXDown = false;
    boolean wasTDown = false;
    boolean wasGDown = false;
    boolean wasLDown = false;
    boolean wasUpDown = false;
    boolean wasDownDown = false;
    boolean wasEDown = false;
    boolean wasCDown = false;
    boolean wasSDown = false;
    boolean wasRDown = false;
    boolean wasWDown = false;
    boolean wasQDown = false;
    boolean wasF1Down = false;
    boolean wasF4Down = false;
    boolean wasF5Down = false;
    public static boolean isClosing = false;

    public static long lastBlockBroken = 0L;
    public static int reach = 50;

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

                if (timeMillis - lastBlockBroken >= 200) { //two tenth second minimum delay between breaking blocks
                    boolean lmbDown = mouseInput.isLeftButtonPressed();
                    boolean mmbDown = mouseInput.isMiddleButtonPressed();
                    boolean rmbDown = mouseInput.isRightButtonPressed();
                    if (lmbDown || mmbDown || rmbDown) {
                        Vector3f pos = raycast(new Matrix4f(player.getCameraMatrix()), lmbDown || mmbDown, reach);
                        if (pos != null) {
                            if (mmbDown) {
                                voxelColor = World.getVoxel(pos.x, pos.y, pos.z);
                            } else {
                                lastBlockBroken = timeMillis;
                                if (lmbDown) {
                                    World.setVoxel(pos.x, pos.y, pos.z, new Vector4f(0));
                                } else if (rmbDown) {
                                    World.setVoxel(pos.x, pos.y, pos.z, voxelColor);
                                }
                            }
                        }
                    }
                }

                if (wasF1Down && !window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS)) {
                    Renderer.showUI = !Renderer.showUI;
                }

                if (window.isKeyPressed(GLFW_KEY_F11, GLFW_PRESS)) {
                    glfwSetWindowPos(window.getWindowHandle(), 0, 0);
                    glfwSetWindowSize(window.getWindowHandle(), 2560, 1440);
                    //glfwSetWindowMonitor(window.getWindowHandle(), glfwGetWindowMonitor(window.getWindowHandle()), 0, 0, 2560, 1440, GLFW_DONT_CARE);
                }

                wasF1Down = window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS);
                wasF4Down = window.isKeyPressed(GLFW_KEY_F4, GLFW_PRESS);
                wasF5Down = window.isKeyPressed(GLFW_KEY_F5, GLFW_PRESS);
                wasQDown = window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS);
                wasEDown = window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS);
                wasCDown = window.isKeyPressed(GLFW_KEY_C, GLFW_PRESS);
                wasSDown = window.isKeyPressed(GLFW_KEY_S, GLFW_PRESS);
                wasRDown = window.isKeyPressed(GLFW_KEY_R, GLFW_PRESS);
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

    public static boolean renderingEnabled = false;
    public static double interpolationTime = 0;
    public static double timePassed = 0;
    public static double tickTime = 50;
    public static long timeMS;
    public static long currentTick = 0;

    public void update(Window window, long diffTimeMillis, long time) throws Exception {
        timeMS = time;
        if (isClosing) {
            glfwSetWindowShouldClose(window.getWindowHandle(), true);
        } else {
            if (!renderingEnabled) {
                renderingEnabled = true;
                Renderer.init(window);
            }
            if (renderingEnabled) {
                interpolationTime = timePassed/tickTime;
                int ticksDone = 0;
                while (timePassed >= tickTime) {
                    ticksDone++;
                    currentTick++;
                    timePassed -= tickTime;
                    interpolationTime = timePassed/tickTime;
                    player.tick();
                    if (ticksDone >= 3) {
                        timePassed = tickTime-1;
                    }
                }
                timePassed += diffTimeMillis;
            }
        }
    }
    public static Vector3f stepMask(Vector3f sideDist) {
        Vector3i mask = new Vector3i();
        Vector3i b1 = new Vector3i(sideDist.x < sideDist.y ? 1 : 0, sideDist.y < sideDist.z ? 1 : 0, sideDist.z < sideDist.x ? 1 : 0);
        Vector3i b2 = new Vector3i(sideDist.x < sideDist.z ? 1 : 0, sideDist.y < sideDist.x ? 1 : 0, sideDist.z < sideDist.y ? 1 : 0);
        mask.z = b1.z > 0 && b2.z > 0 ? 1 : 0;
        mask.x = b1.x > 0 && b2.x > 0 ? 1 : 0;
        mask.y = b1.y > 0 && b2.y > 0 ? 1 : 0;
        if (!(mask.x == 1 || mask.y == 1 || mask.z == 1)) {
            mask.z = 1;
        }

        return new Vector3f(mask);
    }

    public static Vector4f getVoxel(Vector3f realPos) {
        if (realPos.y < 0) {
            return new Vector4f(1);
        }
        return World.getVoxel(realPos.x, realPos.y, realPos.z);
    }

    public static Vector3f raycast(Matrix4f ray, boolean prevPos, int range) { //prevPos is inverted
        Vector3f prevRayPos = new Vector3f(ray.m30(), ray.m31(), ray.m32());

        Matrix4f forwarded = new Matrix4f(ray).translate(0, 0, 10000);
        Vector3f rayPos = new Vector3f(prevRayPos).floor();
        Vector3f rayDir = new Vector3f(new Vector3f((forwarded.m30())-prevRayPos.x, (forwarded.m31())-prevRayPos.y, (forwarded.m32())-prevRayPos.z));
        Vector3f raySign = new Vector3f(Math.signum(rayDir.x), Math.signum(rayDir.y), Math.signum(rayDir.z));
        Vector3f deltaDist = new Vector3f(1/rayDir.x, 1/rayDir.y, 1/rayDir.z);
        Vector3f sideDist = new Vector3f(rayPos).sub(rayPos.x, rayPos.y, rayPos.z).add(0.5f, 0.5f, 0.5f).add(raySign).mul(0.5f, 0.5f, 0.5f).mul(deltaDist);
        Vector3f mask;

        for (int i = 0; i < range; i++) {
            Vector4f voxel = getVoxel(rayPos);
            if (voxel.w > 0.f) {
                if (prevPos) {
                    return rayPos;
                } else {
                    return prevRayPos;
                }
            }
            prevRayPos.set(rayPos);
            mask = stepMask(sideDist);
            rayPos.add(new Vector3f(mask).mul(raySign));
            sideDist.add(new Vector3f(mask).mul(raySign).mul(deltaDist));
        }
        return null;
    }
}