package org.conspiracraft;

import org.conspiracraft.renderer.Renderer;
import org.conspiracraft.renderer.Window;
import org.lwjgl.sdl.SDL_Event;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.conspiracraft.Settings.*;
import static org.conspiracraft.Constants.*;

public class Main {
    public static String mainFolder = System.getenv("APPDATA")+"/Conspiracraft/";
    public static String resourcesPath = mainFolder+"resources/";

    public static Window window;
    public static SDL_Event events;

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
        while (!isClosing) {
            window.update();
            Renderer.render();
        }
        window.cleanup();
    }
}