package org.conspiracraft.engine;

import org.lwjgl.glfw.GLFW;
import org.conspiracraft.Main;

public class Engine {

    public static final int TARGET_UPS = 30;
    private final Main main;
    public final Window window;
    private boolean running;
    private int targetFps;
    private int targetUps;
    public static int fps;

    public Engine(String windowTitle, Window.WindowOptions opts, Main main) throws Exception {
        window = new Window(windowTitle, opts, () -> {
            resize();
            return null;
        });
        targetFps = opts.fps;
        targetUps = opts.ups;
        this.main = main;
        main.init(window);
        running = true;
    }

    private void cleanup() {
        window.cleanup();
    }

    private void resize() {
        //resize stuff
    }

    private void run() throws Exception {
        long initialTime = System.currentTimeMillis();
        float timeU = 1000.0f / targetUps;
        float timeR = targetFps > 0 ? 1000.0f / targetFps : 0;
        float deltaUpdate = 0;
        float deltaFps = 0;
        int framesUntilUpdate = 0;

        long updateTime = initialTime;
        while (running && !window.windowShouldClose()) {
            window.pollEvents();

            long now = System.currentTimeMillis();
            deltaUpdate += (now - initialTime) / timeU;
            deltaFps += (now - initialTime) / timeR;

            if (targetFps <= 0 || deltaFps >= 1) {
                main.input(window, now,  now - initialTime);
            }

            if (deltaUpdate >= 1) {
                long diffTimeMillis = now - updateTime;
                main.update(window, diffTimeMillis, now);
                updateTime = now;
                deltaUpdate--;
            }

            if (targetFps <= 0 || deltaFps >= 1) {
                Renderer.render(window);
                deltaFps--;
                window.update();
                framesUntilUpdate--;
                if (framesUntilUpdate <= 0) {
                    fps = (int)(1000f/(now - initialTime));
                    GLFW.glfwSetWindowTitle(window.getWindowHandle(), "Conspiracraft | " + Main.player.blockPos.x+"x,"+Main.player.blockPos.y+"y,"+Main.player.blockPos.z+"z | " + fps + "fps");
                    framesUntilUpdate = 40;
                }
            }
            initialTime = now;
        }

        cleanup();
    }

    public void start() throws Exception {
        running = true;
        run();
    }

    public void stop() {
        running = false;
    }

}
