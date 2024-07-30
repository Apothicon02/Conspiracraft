package org.apothicon.core;

import org.apothicon.core.utilities.Constants;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryUtil;

import java.util.concurrent.atomic.AtomicBoolean;

public class WindowManager {
    private final String title;

    private int lastWidth, width, xPos, lastHeight, height, zPos;
    private long window;

    private boolean resize, vSync;

    private final Matrix4f projectionMatrix;

    public WindowManager(String title, int width, int height, boolean vSync) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.vSync = vSync;
        projectionMatrix = new Matrix4f();
    }

    public void init() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GL40.GL_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GL40.GL_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL40.GL_TRUE);

        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        width = vidMode.width()/2;
        lastWidth = width;
        height = vidMode.height()/2;
        lastHeight = height;
        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, GLFW.GLFW_FALSE);

        window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
        GLFW.glfwSetWindowPos(window, (vidMode.width() - width) / 2,
                (vidMode.height() - height) / 2);
        AtomicBoolean maximized = new AtomicBoolean(false);

        GLFW.glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
            this.width = width;
            this.height = height;
            this.setResize(true);
        });

        GLFW.glfwSetWindowPosCallback(window, (window, xPos, zPos) -> {
            if (!maximized.get()) {
                this.xPos = xPos;
                this.zPos = zPos;
            }
        });

        GLFW.glfwSetKeyCallback(window, (window, key, scanCode, action, mods) -> {
            if (action == GLFW.GLFW_RELEASE) {
                if (key == GLFW.GLFW_KEY_ESCAPE) {
                    GLFW.glfwSetWindowShouldClose(window, true);
                } else if (key == GLFW.GLFW_KEY_F11) {
                    if (width != vidMode.width() && height != vidMode.height()) {
                        lastWidth = width;
                        lastHeight = height;
                        maximized.set(true);
                        GLFW.glfwSetWindowSize(window, vidMode.width(), vidMode.height());
                        GLFW.glfwSetWindowPos(window, (vidMode.width() - width) / 2,
                                (vidMode.height() - height) / 2);
                        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, GLFW.GLFW_TRUE);
                    } else {
                        maximized.set(false);
                        GLFW.glfwSetWindowSize(window, lastWidth, lastHeight);
                        GLFW.glfwSetWindowPos(window, xPos, zPos);
                        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, GLFW.GLFW_FALSE);
                    }
                    this.setResize(true);
                }
            }
        });

        GLFW.glfwMakeContextCurrent(window);

        if (isvSync()) {
            GLFW.glfwSwapInterval(1);
        }

        GLFW.glfwShowWindow(window);

        GL.createCapabilities();

        GL40.glClearColor(0, 0, 0, 0);
        GL40.glEnable(GL40.GL_DEPTH_TEST);
        GL40.glEnable(GL40.GL_STENCIL_TEST);
        //GL40.glEnable(GL40.GL_CULL_FACE);
        //GL40.glCullFace(GL40.GL_BACK);
    }

    public void update() {
        GLFW.glfwSwapBuffers(window);
        GLFW.glfwPollEvents();
    }

    public void cleanup() {
        GLFW.glfwDestroyWindow(window);
    }

    public void setClearColor(float r, float g, float b, float a) {
        GL40.glClearColor(r, g, b, a);
    }

    public boolean isKeyPressed(int keyCode) {
        return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
    }

    public boolean windowShouldClose() {
        return GLFW.glfwWindowShouldClose(window);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        GLFW.glfwSetWindowTitle(window, title);
    }

    public boolean isResize() {
        return resize;
    }

    public void setResize(boolean resize) {
        this.resize = resize;
    }

    public boolean isvSync() {
        return vSync;
    }

    public void setvSync(boolean vSync) {
        this.vSync = vSync;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public long getWindow() {
        return window;
    }

    public Matrix4f updateProjectionMatrix() {
        float aspectRatio = (float) width / height;
        return projectionMatrix.setPerspective(Constants.FOV, aspectRatio, Constants.Z_NEAR, Constants.Z_FAR);
    }

    public Matrix4f updateProjectionMatrix(Matrix4f matrix, int width, int height) {
        float aspectRatio = (float) width / height;
        return matrix.setPerspective(Constants.FOV, aspectRatio, Constants.Z_NEAR, Constants.Z_FAR);
    }
}
