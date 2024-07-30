package org.apothicon.core;

import org.apothicon.Main;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

public class MouseInput {
    private final Vector2d prevPos, pos;
    private final Vector2f displayVec;

    private boolean inWindow = false, lmb = false, rmb = false;

    public MouseInput() {
        prevPos = new Vector2d(-1, -1);
        pos = new Vector2d(0, 0);
        displayVec = new Vector2f();
    }

    public void init() {
        GLFW.glfwSetCursorPosCallback(Main.getWindow().getWindow(), (window, xPos, yPos) -> {
            pos.x = xPos;
            pos.y = yPos;
        });

        GLFW.glfwSetCursorEnterCallback(Main.getWindow().getWindow(), (window, entered) -> {
            inWindow = entered;
        });

        GLFW.glfwSetMouseButtonCallback(Main.getWindow().getWindow(), (window, button, action, mods) -> {
            lmb = button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS;
            rmb = button == GLFW.GLFW_MOUSE_BUTTON_2 && action == GLFW.GLFW_PRESS;
        });
    }

    public void input() {
        displayVec.x = 0;
        displayVec.y = 0;
        if (prevPos.x > 0 && prevPos.y > 0 && inWindow) {
            double x = pos.x - prevPos.x;
            double y = pos.y - prevPos.y;
            boolean rotateX = x != 0;
            boolean rotateY = y != 0;
            if (rotateX) {
                displayVec.y = (float) x;
            }
            if (rotateY) {
                displayVec.x = (float) y;
            }
        }
        prevPos.x = pos.x;
        prevPos.y = pos.y;
    }

    public Vector2f getDisplayVec() {
        return displayVec;
    }

    public boolean isLmb() {
        return lmb;
    }

    public boolean isRmb() {
        return rmb;
    }
}
