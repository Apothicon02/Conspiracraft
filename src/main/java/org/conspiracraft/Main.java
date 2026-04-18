package org.conspiracraft;

import org.conspiracraft.player.Player;
import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.World;
import org.lwjgl.sdl.SDL_Event;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.conspiracraft.Settings.*;

public class Main {
    public static String mainFolder = System.getenv("APPDATA")+"/Conspiracraft/";
    public static String resourcesPath = mainFolder+"resources/";

    public static Window window;
    public static SDL_Event events;
    public static Player player;

    public static boolean isClosing = false;
    public static List<Long> frameTimes = new ArrayList<>(List.of());
    public static double fps = 0;
    public static double ms = 0;
    public static double timeMul = 1;
    public static double interpolationTime = 0;
    public static long timeNs = 0;
    public static double timeMs = 0;
    public static long timeMsLong = 0;
    public static long currentTick = 0;
    static void main() throws IOException {
        ByteBuffer eventContainer = ByteBuffer.allocateDirect(128);
        events = new SDL_Event(eventContainer);
        window = new Window();
        player = new Player();
        double timeAccum = 0;
        long prevTime = System.nanoTime();
        while (!isClosing) {
            long start = System.nanoTime();
            long elapsed = start-prevTime;
            prevTime = start;
            timeNs += elapsed;
            timeMs = timeNs/1000000d;
            timeMsLong = (long)timeMs;
            window.pollEvents();
            player.inputHandler.update();

            long targetFrameTime = 1000000000L / targetFps;
            long tickTime = (long) (50000000d / timeMul);
            int ticksDone = 0;
            timeAccum += elapsed;
            while (timeAccum >= tickTime) {
                ticksDone++;
                currentTick++;
                timeAccum -= tickTime;
                World.worldType.tick();
                player.tick();
                if (ticksDone >= 3) {
                    timeAccum = (tickTime-1);
                }
            }
            interpolationTime = timeAccum/tickTime;

            Renderer.render();

            frameTimes.addLast(elapsed);
            if (frameTimes.size() > 60) {
                frameTimes.removeFirst();
                double avgNs = Utils.averageLongs(frameTimes);
                ms = avgNs/1000000;
                fps = 1000000000 / avgNs;
            }

            long prevCheck = System.nanoTime();
            while (prevCheck-start < targetFrameTime) {
                prevCheck = System.nanoTime();
            }
        }
        Window.graphics.cleanup();
    }
}