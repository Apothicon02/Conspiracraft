package org.conspiracraft.game;

import org.joml.*;
import org.conspiracraft.engine.*;

import java.lang.Math;
import static org.lwjgl.glfw.GLFW.*;

public class Main {
    public static Camera camera = new Camera();
    private static final float MOUSE_SENSITIVITY = 0.1f;
    private static final float MOVEMENT_SPEED = 0.005f;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        Engine gameEng = new Engine("Conspiracraft", new Window.WindowOptions(), main);
        gameEng.start();
    }

    public void init(Window window) throws Exception {
        World.init();
        Renderer.init();
    }

    boolean wasTDown = false;
    boolean wasGDown = false;
    boolean wasLDown = false;
    boolean wasUpDown = false;
    boolean wasDownDown = false;

    public void input(Window window, long diffTimeMillis) {
        float move = diffTimeMillis * MOVEMENT_SPEED;
        if (window.isKeyPressed(GLFW_KEY_LEFT_SHIFT, GLFW_PRESS)) {
            move*=10;
        }
        if (window.isKeyPressed(GLFW_KEY_CAPS_LOCK, GLFW_PRESS)) {
            move*=100;
        }
        if (window.isKeyPressed(GLFW_KEY_W, GLFW_PRESS)) {
            camera.move(0, 0, move);
        } else if (window.isKeyPressed(GLFW_KEY_S, GLFW_PRESS)) {
            camera.move(0, 0, -move);
        }
        if (window.isKeyPressed(GLFW_KEY_A, GLFW_PRESS)) {
            camera.move(-move, 0, 0);
        } else if (window.isKeyPressed(GLFW_KEY_D, GLFW_PRESS)) {
            camera.move(move, 0, 0);
        }
        if (window.isKeyPressed(GLFW_KEY_SPACE, GLFW_PRESS)) {
            camera.move(0, move, 0);
        } else if (window.isKeyPressed(GLFW_KEY_LEFT_CONTROL, GLFW_PRESS)) {
            camera.move(0, -move, 0);
        }

        MouseInput mouseInput = window.getMouseInput();
        if (mouseInput.isRightButtonPressed()) {
            Vector2f displVec = mouseInput.getDisplVec();
            camera.rotate((float) Math.toRadians(displVec.x * MOUSE_SENSITIVITY),
                    (float) Math.toRadians(displVec.y * MOUSE_SENSITIVITY));
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
                updateTime(diffTimeMillis, 6000);
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
    }
}
