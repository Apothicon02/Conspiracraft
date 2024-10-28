package org.terraflat.game;

import org.joml.*;
import org.terraflat.engine.*;
import org.terraflat.game.space.Voxel;
import org.terraflat.game.blocks.Blocks;
import org.terraflat.game.space.Grid;
import org.terraflat.game.space.Grids;

import java.lang.Math;
import static org.lwjgl.glfw.GLFW.*;

public class Main {
    Camera camera = new Camera();
    public static final Renderer renderer = new Renderer();
    private static final float MOUSE_SENSITIVITY = 0.1f;
    private static final float MOVEMENT_SPEED = 0.005f;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        Engine gameEng = new Engine("Terraflat", new Window.WindowOptions(), main);
        gameEng.start();
    }

    public void init(Window window) throws Exception {
        renderer.init();
        Grid sun = Grids.createGrid("sun", new Grid(new Matrix4f().translate(0, 600,  333), 32));
        sun.setVoxel(0, 0, 0, new Voxel(Blocks.SUN));
        Grid moon = Grids.createGrid("moon", new Grid(new Matrix4f().translate(0, 420, -333), 1));
        moon.setVoxel(0, 0, 0, new Voxel(Blocks.GRASS));
        Grid terrain = Grids.createGrid("terrain", new Grid(new Matrix4f().translate(0, 0, 0), 1));
        FastNoiseLite noise = new FastNoiseLite();
        noise.SetNoiseType(FastNoiseLite.NoiseType.Cellular);
        for (int x = -100; x <= 100; x++) {
            for (int z = -100; z <= 100; z++) {
                float baseCellularNoise = noise.GetNoise(x, z);
                for (int y = 128; y >= 0; y--) {
                    double baseGradient = TerraflatMath.gradient(y, 128, 0, 2, -1);
                    if (baseCellularNoise+baseGradient > 0) {
                        terrain.setVoxel(x, y, z, new Voxel(Blocks.GRASS));
                        y = -1; //temporarily prevent terrain from being thick
                    }
                }
            }
        }
    }

    public void input(Window window, long diffTimeMillis) {
        float move = diffTimeMillis * MOVEMENT_SPEED;
        if (window.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
            move*=10;
        }
        if (window.isKeyPressed(GLFW_KEY_CAPS_LOCK)) {
            move*=100;
        }
        if (window.isKeyPressed(GLFW_KEY_W)) {
            camera.moveForward(move);
        } else if (window.isKeyPressed(GLFW_KEY_S)) {
            camera.moveBackwards(move);
        }
        if (window.isKeyPressed(GLFW_KEY_A)) {
            camera.moveLeft(move);
        } else if (window.isKeyPressed(GLFW_KEY_D)) {
            camera.moveRight(move);
        }
        if (window.isKeyPressed(GLFW_KEY_SPACE)) {
            camera.moveUp(move);
        } else if (window.isKeyPressed(GLFW_KEY_LEFT_CONTROL)) {
            camera.moveDown(move);
        }

        MouseInput mouseInput = window.getMouseInput();
        if (mouseInput.isRightButtonPressed()) {
            Vector2f displVec = mouseInput.getDisplVec();
            camera.addRotation((float) Math.toRadians(displVec.x * MOUSE_SENSITIVITY),
                    (float) Math.toRadians(displVec.y * MOUSE_SENSITIVITY));
        }
    }

    public void update(Window window, long diffTimeMillis) {
        //Grids.getGrids().forEach((String name, Grid grid) -> {
        //    Matrix4f gridMatrix = grid.getMatrix();
        //    if (name.equals("sun") || name.equals("moon")) {
        //        gridMatrix.rotateXYZ(new Vector3f(0.001f, 0.002f, 0.005f));
        //        gridMatrix.rotateLocalY(0.001f);
        //        grid.setMatrix(gridMatrix);
        //    }
        //    grid.getChunks().forEach((Byte chunkX, Map<Byte, Map<Byte, Chunk>> chunkXMap) -> {
        //        chunkXMap.forEach((Byte chunkY, Map<Byte, Chunk> chunkYMap) -> {
        //            chunkYMap.forEach((Byte chunkZ, Chunk chunk) -> {
        //                //create a mesh
        //                for (byte x = 0; x < 32; x++) {
        //                    for (byte y = 0; y < 32; y++) {
        //                        for (byte z = 0; z < 32; z++) {
        //                            Voxel voxel = chunk.getVoxel(x, y, z);
        //                            short[] voxelGridPos = Chunk.chunkPosToGridPos(chunkX, chunkY, chunkZ);
        //                            voxelGridPos = new short[] {(short) (voxelGridPos[0]+x), (short) (voxelGridPos[1]+y), (short) (voxelGridPos[2]+z)};
        //                            Matrix4f voxelMatrix = new Matrix4f(gridMatrix).translate(voxelGridPos[0]*2, voxelGridPos[1]*2, voxelGridPos[2]*2).scale(grid.getScale());
        //                            //add voxels' faces to the mesh, excluding ones that have another voxel next to them.
        //                        }
        //                    }
        //                }
        //            });
        //        });
        //    });
        //});

        //Grid grid = Grids.getGrid("terrain");
        //grid.getChunks().forEach((Byte chunkX, Map<Byte, Map<Byte, Chunk>> chunkXMap) -> {
        //    chunkXMap.forEach((Byte chunkY, Map<Byte, Chunk> chunkYMap) -> {
        //        chunkYMap.forEach((Byte chunkZ, Chunk chunk) -> {
        //
        //        });
        //    });
        //});
    }
}
