package org.conspiracraft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.conspiracraft.game.Config;
import org.conspiracraft.game.ScheduledTicker;
import org.conspiracraft.game.gameplay.HandManager;
import org.conspiracraft.game.gameplay.StackManager;
import org.conspiracraft.game.noise.Noises;
import org.conspiracraft.game.gameplay.Player;
import org.conspiracraft.game.audio.AudioController;
import org.conspiracraft.game.Renderer;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.World;
import org.joml.*;
import org.conspiracraft.engine.*;
import org.lwjgl.opengl.GL;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Math;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.lwjgl.glfw.GLFW.*;

public class Main {
    public static String mainFolder = System.getenv("APPDATA")+"/Conspiracraft/";
    public static String resourcesPath = mainFolder+"resources/";
    public static Gson gson = new Gson();
    public static Player player;
    public static float MOUSE_SENSITIVITY = 0.01f;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        Engine gameEng = new Engine("Conspiracraft", new Window.WindowOptions(), main);
        gameEng.start();
    }

    public void init(Window window) throws Exception {
        if (!Files.exists(Path.of(resourcesPath))) {
            Files.createDirectory(Path.of(resourcesPath));
        }
        Config.readConfig();
        Noises.init();
        //debug worldgen / noise
//        float[] debugData = new float[2048*2048];
//        for (int x = 0; x < 2048; x++) {
//            for (int z = 0; z < 2048; z++) {
//                float spiralNoise = Noises.SPIRAL_NOISE.sample(x, z);
//                double spiralGradient = ConspiracraftMath.gradient(72, 72, 63, 0, -1)+ConspiracraftMath.gradient(72, 128, 72, 0, 1);
//                double spiralY = (spiralNoise*spiralGradient);
//                debugData[(x*2048)+z] = (float) (spiralY < 0.5 ? 0 : 1);
//            }
//        }
//        Noises.COHERERENT_NOISE.data = debugData;
        //debug worldgen / noise
        GL.createCapabilities();
        AudioController.init();
        AudioController.setListenerData(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new float[6]);
        Path deletePath = Paths.get(mainFolder + "delete");
        if (Files.exists(deletePath)) {
            Files.walk(deletePath).sorted(Comparator.reverseOrder()).forEach((path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        String playerPath = (World.worldPath + "/player.data");
        if (Files.exists(Path.of(playerPath))) {
            FileInputStream in = new FileInputStream(playerPath);
            int[] data = Utils.flipIntArray(Utils.byteArrayToIntArray(in.readAllBytes()));
            float[] camMatrix = new float[16];
            int i = 0;
            while (i < 16) {
                camMatrix[i] = data[i]/1000f;
                i++;
            }
            Quaternionf pitch = new Quaternionf(data[i++]/1000f, data[i++]/1000f, data[i++]/1000f, data[i++]/1000f);
            Vector3f pos = new Vector3f(data[i++]/1000f, data[i++]/1000f, data[i++]/1000f);
            Vector3f vel = new Vector3f(data[i++]/1000f, data[i++]/1000f, data[i++]/1000f);
            player = new Player(pos);
            player.vel = vel;
            player.setCameraMatrix(camMatrix);
            player.setCameraPitch(pitch);
            player.flying = data[i++] != 0;
            for (int item = 0; item < player.stack.length; item++) {
                player.stack[item] = data[i++];
            }
        } else {
            player = new Player(new Vector3f(World.size-512, 256, World.size-512));
        }
        String globalPath = (World.worldPath + "/world.data");
        if (Files.exists(Path.of(globalPath))) {
            FileInputStream in = new FileInputStream(globalPath);
            int[] data = Utils.flipIntArray(Utils.byteArrayToIntArray(in.readAllBytes()));
            Renderer.time = data[0]/1000f;
            timePassed = data[1]/1000f;
            meridiem = data[2];
        }
        String chunkEmptinessPath = (World.worldPath + "/chunk_emptiness.data");
        if (Files.exists(Path.of(chunkEmptinessPath))) {
            FileInputStream in = new FileInputStream(chunkEmptinessPath);
            World.chunkEmptiness = Utils.flipIntArray(Utils.byteArrayToIntArray(in.readAllBytes()));
        }
        String heightmapPath = (World.worldPath + "/heightmap.data");
        if (Files.exists(Path.of(heightmapPath))) {
            FileInputStream in = new FileInputStream(heightmapPath);
            World.heightmap = Utils.intArrayToShortArray(Utils.byteArrayToIntArray(in.readAllBytes()));
        }
    }

    boolean wasXDown = false;
    boolean wasTDown = false;
    boolean wasGDown = false;
    boolean wasLDown = false;
    boolean wasUpDown = false;
    boolean wasDownDown = false;
    boolean wasEDown = false;
    boolean wasCDown = false;
    boolean wasSDown = false;
    boolean wasRDown = false;
    boolean wasADown = false;
    boolean wasWDown = false;
    boolean wasQDown = false;
    boolean wasF1Down = false;
    boolean wasF4Down = false;
    boolean wasF5Down = false;
    public static boolean isClosing = false;

    public void input(Window window, long timeMillis, long diffTimeMillis) throws IOException {
        if (!isClosing) {
            if (window.isKeyPressed(GLFW_KEY_ESCAPE, GLFW_PRESS)) {
                isClosing = true;
            } else {
                window.getMouseInput().input(window);
                boolean f3Down = window.isKeyPressed(GLFW_KEY_F3, GLFW_PRESS);
                boolean isShiftDown = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT, GLFW_PRESS);
                boolean isCtrlDown = window.isKeyPressed(GLFW_KEY_LEFT_CONTROL, GLFW_PRESS);
                if (!f3Down) {
                    player.sprint = isShiftDown;
                    player.superSprint = window.isKeyPressed(GLFW_KEY_CAPS_LOCK, GLFW_PRESS);
                    player.forward = window.isKeyPressed(GLFW_KEY_W, GLFW_PRESS);
                    player.backward = window.isKeyPressed(GLFW_KEY_S, GLFW_PRESS);
                    player.rightward = window.isKeyPressed(GLFW_KEY_D, GLFW_PRESS);
                    player.leftward = window.isKeyPressed(GLFW_KEY_A, GLFW_PRESS);
                    player.upward = window.isKeyPressed(GLFW_KEY_SPACE, GLFW_PRESS);
                    player.crouching = isCtrlDown;
                    player.downward = isCtrlDown;
                } else if (window.isKeyPressed(GLFW_KEY_C, GLFW_PRESS) && !wasCDown) {
                    player.creative = !player.creative;
                }

                if (window.isKeyPressed(GLFW_KEY_SPACE, GLFW_PRESS) && timeMillis - player.lastJump > 200) { //only jump at most five times a second
                    player.jump = timeMillis;
                }

                MouseInput mouseInput = window.getMouseInput();
                Vector2f displVec = mouseInput.getDisplVec();
                player.rotate((float) Math.toRadians(displVec.x * MOUSE_SENSITIVITY),
                        (float) Math.toRadians(displVec.y * MOUSE_SENSITIVITY));
                HandManager.useHands(timeMillis, mouseInput);
                mouseInput.scroll.set(0.d);

                if (wasF1Down && !window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS)) {
                    Renderer.showUI = !Renderer.showUI;
                }

                if (window.isKeyPressed(GLFW_KEY_F11, GLFW_PRESS)) {
                    glfwSetWindowPos(window.getWindowHandle(), 0, 0);
                    glfwSetWindowSize(window.getWindowHandle(), 2560, 1440);
                    //glfwSetWindowMonitor(window.getWindowHandle(), glfwGetWindowMonitor(window.getWindowHandle()), 0, 0, 2560, 1440, GLFW_DONT_CARE);
                }
                if (f3Down) {
                    if (wasSDown && !window.isKeyPressed(GLFW_KEY_S, GLFW_PRESS)) {
                        Renderer.shadowsEnabled = !Renderer.shadowsEnabled;
                        Config.writeConfig();
                    }
                    if (wasRDown && !window.isKeyPressed(GLFW_KEY_R, GLFW_PRESS)) {
                        Renderer.reflectionsEnabled = !Renderer.reflectionsEnabled;
                        Config.writeConfig();
                    }
                    if (wasADown && !window.isKeyPressed(GLFW_KEY_A, GLFW_PRESS)) {
                        Renderer.aoQuality++;
                        if (Renderer.aoQuality > 3) {
                            Renderer.aoQuality = 0;
                        }
                        Config.writeConfig();
                    }
                    if (window.isKeyPressed(GLFW_KEY_LEFT_SHIFT, GLFW_PRESS)) {
                        if (wasEDown && !window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS)) {
                            player.stack[1] += 1;
                        } else if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
                            int newSubId = player.stack[1] - 1;
                            if (newSubId < 0) {
                                newSubId = 0;
                            }
                            player.stack[1] = newSubId;
                        }
                    } else {
                        if (wasEDown && !window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS)) {
                            int newId = player.stack[0] + 1;
                            if (newId >= BlockTypes.blockTypeMap.size()) {
                                newId = 0;
                            }
                            player.stack[0] = newId;
                        } else if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
                            int newId = player.stack[0] - 1;
                            if (newId < 0) {
                                newId = BlockTypes.blockTypeMap.size() - 1;
                            }
                            player.stack[0] = newId;
                        }
                    }
                    if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
                        Renderer.atlasChanged = true;
                    }
                    if (wasUpDown && !window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS)) {
                        if (isShiftDown) {
                            timeMul+=0.25f;
                        } else {
                            if (Renderer.renderDistanceMul < 200) {
                                Renderer.renderDistanceMul++;
                                Config.writeConfig();
                            }
                        }
                    }
                    if (wasDownDown && !window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS)) {
                        if (isShiftDown) {
                            timeMul-=0.25f;
                        } else {
                            if (Renderer.renderDistanceMul > 0) {
                                Renderer.renderDistanceMul--;
                                Config.writeConfig();
                            }
                        }
                    }
                } else {
                    if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
                        StackManager.dropStackToGround();
                    }
                    if (wasXDown && !window.isKeyPressed(GLFW_KEY_X, GLFW_PRESS)) {
                        player.flying = !player.flying;
                    }
                    if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
                        updateTime(100000L, 1);
                    }
                }

                wasF1Down = window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS);
                wasF4Down = window.isKeyPressed(GLFW_KEY_F4, GLFW_PRESS);
                wasF5Down = window.isKeyPressed(GLFW_KEY_F5, GLFW_PRESS);
                wasQDown = window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS);
                wasEDown = window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS);
                wasCDown = window.isKeyPressed(GLFW_KEY_C, GLFW_PRESS);
                wasSDown = window.isKeyPressed(GLFW_KEY_S, GLFW_PRESS);
                wasRDown = window.isKeyPressed(GLFW_KEY_R, GLFW_PRESS);
                wasADown = window.isKeyPressed(GLFW_KEY_A, GLFW_PRESS);
                wasWDown = window.isKeyPressed(GLFW_KEY_W, GLFW_PRESS);
                wasTDown = window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS);
                wasXDown = window.isKeyPressed(GLFW_KEY_X, GLFW_PRESS);
                wasGDown = window.isKeyPressed(GLFW_KEY_G, GLFW_PRESS);
                wasLDown = window.isKeyPressed(GLFW_KEY_L, GLFW_PRESS);
                wasUpDown = window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS);
                wasDownDown = window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS);
            }
        }
    }

    public static int meridiem = 1;

    public void updateTime(long diffTimeMillis, float mul) {
        Renderer.time += (diffTimeMillis/600000f)*mul;
        float time = Renderer.timeOfDay+((diffTimeMillis/600000f)*mul)*meridiem;
        if (time < 0f) {
            time = 0;
            meridiem = 1;
        } else if (time > 1f) {
            time = 1;
            meridiem = -1;
        }
        Renderer.timeOfDay = time;
    }

    public static boolean renderingEnabled = false;
    public static double interpolationTime = 0;
    public static double timePassed = 0;
    public static double timeMul = 1;
    public static double tickTime = 50; //50 = base
    public static long timeMS;
    public static long currentTick = 0;

    public void update(Window window, long diffTimeMillis, long time) throws Exception {
        tickTime=50/timeMul;
        timeMS = time;
        if (isClosing) {
            World.saveWorld(World.worldPath+"/");
            Config.writeConfig();
            glfwSetWindowShouldClose(window.getWindowHandle(), true);
        } else {
            if (World.worldGenerated) {
                if (!renderingEnabled) {
                    renderingEnabled = true;
                    Renderer.init(window);
                }
            } else {
                World.generate();
            }
            if (World.worldGenerated && renderingEnabled) {
                updateTime(diffTimeMillis, (float) timeMul);
                int ticksDone = 0;
                while (timePassed >= tickTime) {
                    ticksDone++;
                    currentTick++;
                    timePassed -= tickTime;
                    World.tick();
                    player.tick();
                    ScheduledTicker.tick();
                    AudioController.disposeSources();
                    if (ticksDone >= 3) {
                        timePassed = tickTime-1;
                    }
                }
                interpolationTime = timePassed/tickTime;
                World.iterateLightQueue();
                timePassed += diffTimeMillis;
            }
        }
    }
}