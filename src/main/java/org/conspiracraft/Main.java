package org.conspiracraft;

import org.conspiracraft.game.ScheduledTicker;
import org.conspiracraft.game.blocks.types.BlockProperties;
import org.conspiracraft.game.interactions.Handcrafting;
import org.conspiracraft.game.noise.Noises;
import org.conspiracraft.game.Player;
import org.conspiracraft.game.audio.AudioController;
import org.conspiracraft.game.Renderer;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.world.World;
import org.joml.*;
import org.conspiracraft.engine.*;
import org.lwjgl.opengl.GL;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Math;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.conspiracraft.game.Player.selectedBlock;
import static org.lwjgl.glfw.GLFW.*;

public class Main {
    public static Player player;
    private static final float MOUSE_SENSITIVITY = 0.01f;
    private static final float MOVEMENT_SPEED = 0.005f;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        Engine gameEng = new Engine("Conspiracraft", new Window.WindowOptions(), main);
        gameEng.start();
    }

    public void init(Window window) throws Exception {
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
        AudioController.loadSound("jump.wav");
        AudioController.loadSound("grass_step1.wav");
        AudioController.loadSound("grass_step2.wav");
        AudioController.loadSound("grass_step3.wav");
        AudioController.loadSound("dirt_step1.wav");
        AudioController.loadSound("dirt_step2.wav");
        AudioController.loadSound("dirt_step3.wav");
        AudioController.loadSound("swim1.wav");
        AudioController.loadSound("splash1.wav");
        AudioController.loadSound("flow.wav");
        AudioController.loadSound("wind.wav");
        AudioController.loadSound("buzz.wav");
        AudioController.loadSound("chirp_1.wav");
        AudioController.loadSound("magma.wav");
        AudioController.loadRandomSound("Music/");
        Path deletePath = Paths.get(System.getenv("APPDATA") + "/Conspiracraft/delete");
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
            selectedBlock.x = data[i++];
            selectedBlock.y = data[i++];
            selectedBlock.z = data[i++];
        } else {
            player = new Player(new Vector3f(512, 256, 512));
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
    boolean wasRDown = false;
    boolean wasWDown = false;
    boolean wasQDown = false;
    boolean wasF1Down = false;
    boolean wasF4Down = false;
    boolean wasF5Down = false;
    public static boolean isClosing = false;

    public static long lastBlockBroken = 0L;
    public static int reach = 50;
    public static float reachAccuracy = 200;

    public void input(Window window, long timeMillis, long diffTimeMillis) {
        if (!isClosing) {
            if (window.isKeyPressed(GLFW_KEY_ESCAPE, GLFW_PRESS)) {
                isClosing = true;
            } else {
                window.getMouseInput().input(window);
                boolean isShiftDown = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT, GLFW_PRESS);
                player.sprint = isShiftDown;
                player.superSprint = window.isKeyPressed(GLFW_KEY_CAPS_LOCK, GLFW_PRESS);
                player.forward = window.isKeyPressed(GLFW_KEY_W, GLFW_PRESS);
                player.backward = window.isKeyPressed(GLFW_KEY_S, GLFW_PRESS);
                player.rightward = window.isKeyPressed(GLFW_KEY_D, GLFW_PRESS);
                player.leftward = window.isKeyPressed(GLFW_KEY_A, GLFW_PRESS);
                player.upward = window.isKeyPressed(GLFW_KEY_SPACE, GLFW_PRESS);
                player.downward = window.isKeyPressed(GLFW_KEY_LEFT_CONTROL, GLFW_PRESS);

                if (window.isKeyPressed(GLFW_KEY_SPACE, GLFW_PRESS) && timeMillis - player.lastJump > 200) { //only jump at most five times a second
                    player.jump = timeMillis;
                }

                MouseInput mouseInput = window.getMouseInput();
                Vector2f displVec = mouseInput.getDisplVec();
                player.rotate((float) Math.toRadians(displVec.x * MOUSE_SENSITIVITY),
                        (float) Math.toRadians(displVec.y * MOUSE_SENSITIVITY));
                if (timeMillis - lastBlockBroken >= 200) { //two tenth second minimum delay between breaking blocks
                    boolean lmbDown = mouseInput.isLeftButtonPressed();
                    boolean mmbDown = mouseInput.isMiddleButtonPressed();
                    boolean rmbDown = mouseInput.isRightButtonPressed();
                    if (lmbDown || mmbDown || rmbDown) {
                        Vector3f pos = raycast(new Matrix4f(player.getCameraMatrix()), lmbDown || mmbDown, reach, mmbDown, reachAccuracy);
                        if (pos != null) {
                            if (mmbDown) {
                                if (isShiftDown) {
                                    Vector2i block = World.getBlock(pos.x, pos.y, pos.z);
                                    if (block != null) {
                                        selectedBlock = Handcrafting.interact(selectedBlock, block);
                                    }
                                } else {
                                    Vector2i block = World.getBlock(pos.x, pos.y, pos.z);
                                    if (block != null) {
                                        selectedBlock = new Vector3i(block.x, BlockTypes.blockTypeMap.get(block.x).blockProperties.isFluid ? 0 : block.y, 15);
                                    }
                                }
                            } else if (BlockTypes.blockTypeMap.get(selectedBlock.x()) != null) {
                                lastBlockBroken = timeMillis;
                                int blockTypeId = selectedBlock.x();
                                int blockSubtypeId = selectedBlock.y();
                                int amount = selectedBlock.z();
                                int cornerData = World.getCorner((int) pos.x, (int) pos.y, (int) pos.z);
                                int cornerIndex = (pos.y < (int)(pos.y)+0.5 ? 0 : 4) + (pos.z < (int)(pos.z)+0.5 ? 0 : 2) + (pos.x < (int)(pos.x)+0.5 ? 0 : 1);
                                if (lmbDown) {
                                    cornerData |= (1 << (cornerIndex - 1));
                                    if (cornerData == -2147483521 || !isShiftDown) {
                                        Vector2i blockBreaking = World.getBlock(pos.x, pos.y, pos.z);
                                        if (amount == 0 || (blockTypeId == blockBreaking.x && amount < 15)) {
                                            selectedBlock = new Vector3i(blockBreaking.x, BlockTypes.blockTypeMap.get(blockBreaking.x).blockProperties.isFluid ? 0 : blockBreaking.y, amount+1);
                                            World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, 0);
                                            blockTypeId = 0;
                                            blockSubtypeId = 0;
                                            World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockTypeId, blockSubtypeId, true, false, 1, false);
                                        }
                                    } else {
                                        World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, cornerData);
                                    }
                                } else if (rmbDown) {
                                    if (cornerData != 0) {
                                        cornerData &= (~(1 << (cornerIndex - 1)));
                                        World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, cornerData);
                                    } else if (amount > 0) {
                                        Vector2i oldBlock = World.getBlock((int) pos.x, (int) pos.y, (int) pos.z);
                                        BlockProperties oldType = BlockTypes.blockTypeMap.get(oldBlock.x).blockProperties;
                                        BlockProperties selectedProperties = BlockTypes.blockTypeMap.get(selectedBlock.x).blockProperties;
                                        if (oldType.isFluidReplaceable) {
                                            World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockTypeId, BlockTypes.blockTypeMap.get(blockTypeId).blockProperties.isFluid ? amount : blockSubtypeId, true, false, 1, false);
                                            boolean isFluid = BlockTypes.blockTypeMap.get(selectedBlock.x).blockProperties.isFluid;
                                            if (!(isFluid && isShiftDown)) {
                                                selectedBlock.z -= BlockTypes.blockTypeMap.get(blockTypeId).blockProperties.isFluid ? (amount - oldBlock.y) : 1;
                                            }

                                            if (selectedBlock.z <= 0) {
                                                if (isFluid) {
                                                    selectedBlock.x = 22;
                                                    selectedBlock.y = 0;
                                                    selectedBlock.z = 1;
                                                } else {
                                                    selectedBlock.x = 0;
                                                    selectedBlock.y = 0;
                                                }
                                            }
                                        } else if (selectedProperties.isFluid && oldBlock.x == selectedBlock.x) {
                                            World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockTypeId, Math.min(15, oldBlock.y()+amount), true, false, 1, false);
                                            selectedBlock.z -= (amount - oldBlock.y);
                                            if (selectedBlock.z <= 0) {
                                                selectedBlock.x = 22;
                                                selectedBlock.y = 0;
                                                selectedBlock.z = 1;
                                            }
                                        } else if (!selectedProperties.isFluidReplaceable && oldType.isFluid) {
                                            //displace all fluid to first of 6 neighbors, if impossible then block will not be placed.
                                            World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockTypeId, blockSubtypeId, true, false, 1, false);
                                            selectedBlock.z--;
                                            if (selectedBlock.z <= 0) {
                                                selectedBlock.x = 0;
                                                selectedBlock.y = 0;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (wasF1Down && !window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS)) {
                    Renderer.showUI = !Renderer.showUI;
                }
                if (wasF4Down && !window.isKeyPressed(GLFW_KEY_F4, GLFW_PRESS)) {
                    Renderer.shadowsEnabled = !Renderer.shadowsEnabled;
                }
                if (wasF5Down && !window.isKeyPressed(GLFW_KEY_F5, GLFW_PRESS)) {
                    Renderer.reflectionsEnabled = !Renderer.reflectionsEnabled;
                }

                if (window.isKeyPressed(GLFW_KEY_F11, GLFW_PRESS)) {
                    glfwSetWindowPos(window.getWindowHandle(), 0, 0);
                    glfwSetWindowSize(window.getWindowHandle(), 2560, 1440);
                    //glfwSetWindowMonitor(window.getWindowHandle(), glfwGetWindowMonitor(window.getWindowHandle()), 0, 0, 2560, 1440, GLFW_DONT_CARE);
                }
                if (window.isKeyPressed(GLFW_KEY_F3, GLFW_PRESS)) {
                    if (wasRDown && !window.isKeyPressed(GLFW_KEY_R, GLFW_PRESS)) {
                        Renderer.reflectionsEnabled = !Renderer.reflectionsEnabled;
                    }
                    if (wasCDown && !window.isKeyPressed(GLFW_KEY_C, GLFW_PRESS)) {
                        Renderer.cloudsEnabled = !Renderer.cloudsEnabled;
                    }
                    if (wasEDown && !window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS)) {
                        selectedBlock.add(new Vector3i(0, isShiftDown ? 10 : 1, 0));
                    } else if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
                        int newSubId = selectedBlock.y() - (isShiftDown ? 10 : 1);
                        if (newSubId < 0) {
                            newSubId = 0;
                        }
                        selectedBlock = new Vector3i(selectedBlock.x, newSubId, selectedBlock.z());
                    }
                    if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
                        Renderer.worldChanged = true;
                        Renderer.atlasChanged = true;
                    }
                    if (wasUpDown && !window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS)) {
                        if (Renderer.renderDistanceMul < 200) {
                            Renderer.renderDistanceMul++;
                        }
                    }
                    if (wasDownDown && !window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS)) {
                        if (Renderer.renderDistanceMul > 0) {
                            Renderer.renderDistanceMul--;
                        }
                    }
                } else {
                    if (wasEDown && !window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS)) {
                        int newId = selectedBlock.x() + 1;
                        if (newId >= BlockTypes.blockTypeMap.size()) {
                            newId = 0;
                        }
                        selectedBlock = new Vector3i(newId, selectedBlock.y, selectedBlock.z());
                    } else if (wasQDown && !window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS)) {
                        int newId = selectedBlock.x() - 1;
                        if (newId < 0) {
                            newId = BlockTypes.blockTypeMap.size() - 1;
                        }
                        selectedBlock = new Vector3i(newId, selectedBlock.y, selectedBlock.z());
                    }
                    if (wasXDown && !window.isKeyPressed(GLFW_KEY_X, GLFW_PRESS)) {
                        player.flying = !player.flying;
                    }
                    if (wasTDown && !window.isKeyPressed(GLFW_KEY_T, GLFW_PRESS)) {
                        updateTime(100000L, 1);
                    }
                    if (wasUpDown && !window.isKeyPressed(GLFW_KEY_UP, GLFW_PRESS)) {
                        if (isShiftDown) {
                            selectedBlock.z = 16;
                        } else {
                            selectedBlock.z++;
                            if (selectedBlock.z > 16) {
                                selectedBlock.z = 16;
                            }
                        }
                        if (BlockTypes.blockTypeMap.get(selectedBlock.x).blockProperties.isFluid && selectedBlock.z > 15) {
                            selectedBlock.z = 15;
                        }
                    }
                    if (wasDownDown && !window.isKeyPressed(GLFW_KEY_DOWN, GLFW_PRESS)) {
                        selectedBlock.z--;
                        if (selectedBlock.z < 0) {
                            selectedBlock.z = 0;
                        }
                    }
                }

                wasF1Down = window.isKeyPressed(GLFW_KEY_F1, GLFW_PRESS);
                wasF4Down = window.isKeyPressed(GLFW_KEY_F4, GLFW_PRESS);
                wasF5Down = window.isKeyPressed(GLFW_KEY_F5, GLFW_PRESS);
                wasQDown = window.isKeyPressed(GLFW_KEY_Q, GLFW_PRESS);
                wasEDown = window.isKeyPressed(GLFW_KEY_E, GLFW_PRESS);
                wasCDown = window.isKeyPressed(GLFW_KEY_C, GLFW_PRESS);
                wasRDown = window.isKeyPressed(GLFW_KEY_R, GLFW_PRESS);
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
    public static double tickTime = 50;
    public static long timeMS;
    public static long currentTick = 0;

    public void update(Window window, long diffTimeMillis, long time) throws Exception {
        timeMS = time;
        if (isClosing) {
            World.saveWorld(World.worldPath+"/");
            glfwSetWindowShouldClose(window.getWindowHandle(), true);
        } else {
            if (!renderingEnabled) {
                renderingEnabled = true;
                Renderer.init(window);
            }
            World.run();
            updateTime(diffTimeMillis, 1);
            interpolationTime = timePassed/tickTime;
            while (timePassed >= tickTime) {
                currentTick++;
                timePassed -= tickTime;
                interpolationTime = timePassed/tickTime;
                World.tick();
                player.tick();
                ScheduledTicker.tick();
            }
            timePassed += diffTimeMillis;
        }
    }
    public static Vector3f stepMask(Vector3f sideDist) {
        Vector3i mask = new Vector3i();
        Vector3i b1 = new Vector3i(sideDist.x < sideDist.y ? 1 : 0, sideDist.y < sideDist.z ? 1 : 0, sideDist.z < sideDist.x ? 1 : 0);
        Vector3i b2 = new Vector3i(sideDist.x < sideDist.z ? 1 : 0, sideDist.y < sideDist.x ? 1 : 0, sideDist.z < sideDist.y ? 1 : 0);
        mask.z = b1.z > 0 && b2.z > 0 ? 1 : 0;
        mask.x = b1.x > 0 && b2.x > 0 ? 1 : 0;
        mask.y = b1.y > 0 && b2.y > 0 ? 1 : 0;
        if (!(mask.x == 1 || mask.y == 1 || mask.z == 1)) {
            mask.z = 1;
        }

        return new Vector3f(mask);
    }

    public static Vector3f raycast(Matrix4f ray, boolean prevPos, int range, boolean countFluids, float accuracy) { //prevPos is inverted
        Vector3f prevRayPos = new Vector3f(ray.m30(), ray.m31(), ray.m32());

//        Vector4f uvDir = new Vector4f(new Vector3f(0.5f, 0.5f, 1).normalize(), 0);
//        ray = new Matrix4f(
//                ray.m00(), ray.m10(), ray.m20(), ray.m30(),
//                ray.m01(), ray.m11(), ray.m21(), ray.m31(),
//                ray.m02(), ray.m12(), ray.m22(), ray.m32(),
//                ray.m03(), ray.m13(), ray.m23(), ray.m33()).transpose();
//        ray.mul(new Matrix4f(uvDir, uvDir, uvDir, uvDir));
//        Vector3f rayDir = new Vector3f(ray.m00(), ray.m01(), ray.m02());

        Matrix4f forwarded = new Matrix4f(ray).translate(0, 0, 10000);
        Vector3f rayDir = new Vector3f(new Vector3f(forwarded.m30()-prevRayPos.x, forwarded.m31()-prevRayPos.y, forwarded.m32()-prevRayPos.z));
        Vector3f rayPos = new Vector3f(prevRayPos).floor();
        Vector3f raySign = new Vector3f(Math.signum(rayDir.x), Math.signum(rayDir.y), Math.signum(rayDir.z));
        Vector3f deltaDist = new Vector3f(1/rayDir.x, 1/rayDir.y, 1/rayDir.z);
        Vector3f sideDist = new Vector3f(rayPos).sub(ray.m30(), ray.m31(), ray.m32()).add(0.5f, 0.5f, 0.5f).add(raySign).mul(0.5f, 0.5f, 0.5f).mul(deltaDist);
        Vector3f mask = stepMask(sideDist);

        for (int i = 0; i < range; i++) {
            Vector2i block = World.getBlock(rayPos.x, rayPos.y, rayPos.z);
            if (block != null) {
                int typeId = block.x();
                if (typeId != 0) {
                    int subTypeId = block.y();
                    boolean isFluid = BlockTypes.blockTypeMap.get(typeId).blockProperties.isFluid;
                    if (countFluids || !isFluid) {
                        if (isFluid) {
                            subTypeId = Math.min(20, subTypeId);
                        }
                        int cornerData = World.getCorner((int) rayPos.x, (int) rayPos.y, (int) rayPos.z);
                        int cornerIndex = (rayPos.y < (int) (rayPos.y) + 0.5 ? 0 : 4) + (rayPos.z < (int) (rayPos.z) + 0.5 ? 0 : 2) + (rayPos.x < (int) (rayPos.x) + 0.5 ? 0 : 1);
                        if (((cornerData & (1 << (cornerIndex - 1))) >> (cornerIndex - 1)) == 0) {
                            if (Renderer.collisionData[(1024 * ((typeId * 8) + (int) ((rayPos.x - Math.floor(rayPos.x)) * 8))) + (subTypeId * 64) + ((Math.abs(((int) ((rayPos.y - Math.floor(rayPos.y)) * 8)) - 8) - 1) * 8) + (int) ((rayPos.z - Math.floor(rayPos.z)) * 8)]) {
                                if (prevPos) {
                                    return rayPos;
                                } else {
                                    return prevRayPos;
                                }
                            }
                        }
                    }
                }
            }
            prevRayPos.set(rayPos);
            mask = stepMask(sideDist);
            rayPos.add(new Vector3f(mask).mul(raySign));
            sideDist.add(new Vector3f(mask).mul(raySign).mul(deltaDist));
        }

//        Vector3f prevRayPos = new Vector3f(ray.m30(), ray.m31(), ray.m32());
//        Vector3f rayPos = new Vector3f(0);
//        for (int i = 0; i < range*accuracy; i++) {
//            rayPos.set(ray.m30(), ray.m31(), ray.m32());
//            Vector2i block = World.getBlock(rayPos.x, rayPos.y, rayPos.z);
//            if (block != null) {
//                int typeId = block.x();
//                if (typeId != 0) {
//                    int subTypeId = block.y();
//                    boolean isFluid = BlockTypes.blockTypeMap.get(typeId).blockProperties.isFluid;
//                    if (countFluids || !isFluid) {
//                        if (isFluid) {
//                            subTypeId = Math.min(20, subTypeId);
//                        }
//                        int cornerData = World.getCorner((int) rayPos.x, (int) rayPos.y, (int) rayPos.z);
//                        int cornerIndex = (rayPos.y < (int) (rayPos.y) + 0.5 ? 0 : 4) + (rayPos.z < (int) (rayPos.z) + 0.5 ? 0 : 2) + (rayPos.x < (int) (rayPos.x) + 0.5 ? 0 : 1);
//                        if (((cornerData & (1 << (cornerIndex - 1))) >> (cornerIndex - 1)) == 0) {
//                            if (Renderer.collisionData[(1024 * ((typeId * 8) + (int) ((rayPos.x - Math.floor(rayPos.x)) * 8))) + (subTypeId * 64) + ((Math.abs(((int) ((rayPos.y - Math.floor(rayPos.y)) * 8)) - 8) - 1) * 8) + (int) ((rayPos.z - Math.floor(rayPos.z)) * 8)]) {
//                                if (prevPos) {
//                                    return rayPos;
//                                } else {
//                                    return prevRayPos;
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//            prevRayPos.set(rayPos);
//            ray.translate(0, 0, 0.1f/accuracy);
//        }
        return null;
    }
}