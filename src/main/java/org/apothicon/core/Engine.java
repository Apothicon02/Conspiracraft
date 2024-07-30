package org.apothicon.core;

import org.apothicon.Main;
import org.apothicon.core.utilities.Constants;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

public class Engine {
    public static final long NANOSECOND = 1000000000L;
    public static final float FRAMERATE = 1000;

    private static int fps;
    private static float frameTime = 1.0f / FRAMERATE;

    private boolean isRunning;


    private GLFWErrorCallback errorCallback;
    private WindowManager window;
    private ILogic gameLogic;
    private MouseInput mouseInput;

    private void init() throws Exception {
        GLFW.glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
        window = Main.getWindow();
        gameLogic = Main.getTerraflat();
        mouseInput = new MouseInput();
        window.init();
        gameLogic.init();
        mouseInput.init();
    }

    public void start() throws Exception {
        init();
        if (isRunning) {
            return;
        }
        run();
    }

    public void run() {
        this.isRunning = true;
        int frames = 0;
        long frameCounter = 0;
        long lastTime = System.nanoTime();
        double unprocessedTime = 0;

        while (isRunning) {
            boolean render = false;
            long startTime = System.nanoTime();
            long passedTime = startTime - lastTime;
            lastTime = startTime;

            unprocessedTime += passedTime / (double) NANOSECOND;
            frameCounter += passedTime;

            input();

            while (unprocessedTime > frameTime) {
                render = true;
                unprocessedTime -= frameTime;

                if (window.windowShouldClose()) {
                    stop();
                }

                if (frameCounter >= NANOSECOND) {
                    setFps(frames);
                    window.setTitle(Constants.NAME_OF_THE_GAME + " | " + getFps() + "FPS");
                    frames = 0;
                    frameCounter = 0;
                }
            }
            if (render) {
                update(0);
                render();
                frames++;
            }
        }
        cleanup();
    }

    private int getFps() {
        return fps;
    }

    private void setFps(int frames) {
        fps = frames;
    }

    private void stop() {
        if(!isRunning) {
            return;
        }
        isRunning = false;
    }

    private void input() {
        mouseInput.input();
        gameLogic.input();
    }

    private void render() {
        gameLogic.render();
        window.update();
    }

    private void update(float interval) {
        gameLogic.update(interval, mouseInput);
    }

    private void cleanup() {
        window.cleanup();
        gameLogic.cleanup();
        errorCallback.free();
        GLFW.glfwTerminate();
    }
}
