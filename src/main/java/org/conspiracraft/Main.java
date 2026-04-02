package org.conspiracraft;

import org.conspiracraft.player.Player;
import org.conspiracraft.renderer.Renderer;
import org.conspiracraft.renderer.Window;
import org.joml.Vector2f;
import org.lwjgl.sdl.SDL_Event;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.conspiracraft.Settings.*;
import static org.conspiracraft.Constants.*;
import static org.lwjgl.sdl.SDLScancode.*;
import static org.lwjgl.sdl.SDLVideo.SDL_GetWindowFlags;
import static org.lwjgl.sdl.SDLVideo.SDL_WINDOW_INPUT_FOCUS;

public class Main {
    public static String mainFolder = System.getenv("APPDATA")+"/Conspiracraft/";
    public static String resourcesPath = mainFolder+"resources/";

    public static Window window;
    public static SDL_Event events;
    public static Player player;

    public static boolean isClosing = false;
    public static List<Long> frameTimes = new ArrayList<>(List.of());
    public static double avgMS = 0;

    static void main(String[] args) throws Exception {
        long initialNanoTime = System.nanoTime();
        long initialTime = System.currentTimeMillis();
        float timeU = 1000.0f / TARGET_UPS;
        float timeR = targetFps > 0 ? 1000.0f / targetFps : 0;
        float deltaUpdate = 0;
        float deltaFps = 0;
        int framesUntilUpdate = 0;
        while (frameTimes.size() < 60) {
            frameTimes.add(15000000L);
        }

        ByteBuffer eventContainer = ByteBuffer.allocateDirect(128);
        events = new SDL_Event(eventContainer);
        long updateTime = initialTime;
        window = new Window();
        player = new Player();
        while (!isClosing) {
            window.pollEvents();
            long now = System.currentTimeMillis();
            deltaUpdate += (now - initialTime) / timeU;
            deltaFps += (now - initialTime) / timeR;

            long diffTimeMillis = now - updateTime;
            Main.timeMS += (long) (diffTimeMillis*Main.timeMul);

            player.inputHandler.update();

            update(diffTimeMillis);
            updateTime = now;
            if (deltaUpdate >= 1) {
                deltaUpdate--;
            }

            if (targetFps <= 0 || deltaFps >= 1) {
                deltaFps--;
                framesUntilUpdate--;
                if (framesUntilUpdate <= 0) {
                    avgMS = 1000000000d/Utils.averageLongs(frameTimes);
                    framesUntilUpdate = 40;
                }
                long diffTimeNanos = (System.nanoTime() - initialNanoTime);
                frameTimes.addLast(diffTimeNanos);
                if (frameTimes.size() > 60) {
                    frameTimes.removeFirst();
                }
                initialNanoTime = System.nanoTime();
            }
            initialTime = now;
        }
        window.cleanup();
    }

    public static double interpolationTime = 0;
    public static double timePassed = 0;
    public static double timeMul = 1;
    public static double tickTime = 50;
    public static long timeMS;
    public static long currentTick = 0;
    public static void update(long diffTimeMillis) throws Exception {
        tickTime = 50 / timeMul;
        int ticksDone = 0;
        while (timePassed >= tickTime) {
            ticksDone++;
            currentTick++;
            timePassed -= tickTime;
            player.tick();
            if (ticksDone >= 3) {
                timePassed = tickTime-1;
            }
        }
        interpolationTime = timePassed/tickTime;
        Renderer.render();
        timePassed += diffTimeMillis;
    }
}