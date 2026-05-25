package org.conspiracraft;

import org.conspiracraft.audio.AudioController;
import org.conspiracraft.player.Player;
import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.LightHelper;
import org.conspiracraft.world.World;
import org.lwjgl.sdl.SDL_Event;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.conspiracraft.Settings.*;

public class Main {
    public static String mainFolder = System.getenv("APPDATA")+"/Conspiracraft/";
    public static String resourcesPath = mainFolder+"resources/";

    public static Window window;
    public static SDL_Event events;
    public static Player player;

    public static boolean isClosing = false;
    public static boolean isSaving = false;
    public static List<Long> frameTimes = new ArrayList<>(List.of());
    public static double fps = 0;
    public static double ms = 0;
    public static double timeMul = 1;
    public static double interpolationTime = 0;
    public static long timeNs = 0;
    public static double timeMs = 0;
    public static long timeMsLong = 0;
    public static long currentTick = 0;
    static void main() throws Exception {
        //int l = Chunk.packLight(0, 0, 0, maxSunlightLevel);
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception in thread " + thread.getName());
            throwable.printStackTrace();
        });
        ByteBuffer eventContainer = ByteBuffer.allocateDirect(128);
        events = new SDL_Event(eventContainer);
        window = new Window();
        AudioController.init();
        Files.createDirectories(Path.of(mainFolder));
        copyAssets();
        Settings.load();
        player = new Player();
        player.create();
        World.load(World.worldType.getWorldPath() + "/");
        double timeAccum = 0;
        long prevTime = System.nanoTime();
        while (!isClosing) {
            long start = System.nanoTime();
            long elapsed = start-prevTime;
            prevTime = start;
            timeNs += (long) (elapsed*timeMul);
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
                World.tick();
                World.worldType.tick();
                World.tickItems();
                player.tick();
                if (ticksDone >= 3) {
                    timeAccum = (tickTime-1);
                }
            }
            interpolationTime = timeAccum/tickTime;

            LightHelper.iterateLightQueue();
            Renderer.render();
            AudioController.tick();

            if (isSaving) {
                Main.save();
            }

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

    public static void save() throws IOException {
        Settings.save();
        player.save();
        World.save(World.worldType.getWorldPath() + "/");
        isSaving = false;
    }

    public static void copyAssets() throws IOException, URISyntaxException {
        Path targetDir = Paths.get(mainFolder+"assets/");
        targetDir.toFile().mkdirs();
        Files.createDirectories(targetDir);

        URI uri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        Path jarPath = Paths.get(uri);
        if (!jarPath.toString().endsWith(".jar")) {return;} //don't copy assets if not running from jar
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!name.startsWith("assets/")) continue;
                if (entry.isDirectory()) continue;

                Path outPath = targetDir.resolve(name.substring("assets/".length()));
                Files.createDirectories(outPath.getParent());

                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, outPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}