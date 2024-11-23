package org.conspiracraft.engine;

import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;

public class MouseInput {

    private Vector2f currentPos;
    private Vector2f displVec;
    private boolean inWindow;
    private boolean leftButtonPressed;
    private boolean middleButtonPressed;
    private Vector2f previousPos;
    private boolean rightButtonPressed;

    public MouseInput(Window window) {
        previousPos = new Vector2f(-1, -1);
        long windowHandle = window.getWindowHandle();
        currentPos = new Vector2f();
        displVec = new Vector2f();
        leftButtonPressed = false;
        middleButtonPressed = false;
        rightButtonPressed = false;
        inWindow = false;

        glfwSetCursorEnterCallback(windowHandle, (handle, entered) -> inWindow = entered);
        glfwSetMouseButtonCallback(windowHandle, (handle, button, action, mode) -> {
            leftButtonPressed = button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS;
            middleButtonPressed = button == GLFW_MOUSE_BUTTON_MIDDLE && action == GLFW_PRESS;
            rightButtonPressed = button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_PRESS;
        });
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
    }

    public Vector2f getCurrentPos() {
        return currentPos;
    }

    public Vector2f getDisplVec() {
        return displVec;
    }

    public void input(Window window) {
        double[] x = new double[1];
        double[] y = new double[1];
        glfwGetCursorPos(window.getWindowHandle(), x, y);
        currentPos.x = (float) x[0];
        currentPos.y = (float) y[0];
        displVec.x = 0;
        displVec.y = 0;
        if (previousPos.x > 0 && previousPos.y > 0 && inWindow) {
            float deltax = currentPos.x - previousPos.x;
            float deltay = currentPos.y - previousPos.y;
            boolean rotateX = deltax != 0;
            boolean rotateY = deltay != 0;
            if (rotateX) {
                displVec.y = deltax;
            }
            if (rotateY) {
                displVec.x = deltay;
            }
        }
        previousPos = new Vector2f(window.getWidth()/2f, window.getHeight()/2f);
        glfwSetCursorPos(window.getWindowHandle(), previousPos.x, previousPos.y);
    }

    public boolean isLeftButtonPressed() {
        return leftButtonPressed;
    }
    public boolean isMiddleButtonPressed() {
        return middleButtonPressed;
    }
    public boolean isRightButtonPressed() {
        return rightButtonPressed;
    }
}
