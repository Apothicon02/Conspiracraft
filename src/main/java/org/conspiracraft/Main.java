package org.conspiracraft;

import org.conspiracraft.engine.*;
import org.conspiracraft.game.World;
import org.lwjgl.opengl.GL;


public class Main {
    private static final float MOUSE_SENSITIVITY = 0.01f;
    private static final float MOVEMENT_SPEED = 0.005f;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        Engine gameEng = new Engine("Conspiracraft", new Window.WindowOptions(), main);
        gameEng.start();
    }

    public void init(Window window) throws Exception {
        GL.createCapabilities();
        Renderer.init(window);
        World.init();
    }

    public void input(Window window, long timeMillis, long diffTimeMillis) {

    }

    public static double timePassed = 0;
    public static double tickTime = 25;
    public static float worldTickStage = 0f;

    public void update(Window window, long diffTimeMillis, long time) throws Exception {
        while (timePassed >= tickTime) {
            timePassed -= tickTime;
            worldTickStage+=0.025f;
            if (worldTickStage >= 0.976f) {
                worldTickStage = 0f;
            }
            World.tick(worldTickStage);
        }
        timePassed += diffTimeMillis;
    }
}
