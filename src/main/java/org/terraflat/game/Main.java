package org.terraflat.game;

import org.joml.*;
import org.lwjgl.opengl.GL40;
import org.terraflat.engine.*;
import org.terraflat.engine.graph.*;
import org.terraflat.engine.scene.*;
import org.terraflat.game.elements.Element;
import org.terraflat.game.elements.Models;
import org.terraflat.game.grids.Grid;
import org.terraflat.game.grids.Grids;

import java.awt.geom.AffineTransform;
import java.lang.Math;

import static org.lwjgl.glfw.GLFW.*;

public class Main implements IAppLogic {

    private static final float MOUSE_SENSITIVITY = 0.1f;
    private static final float MOVEMENT_SPEED = 0.005f;

    public static void main(String[] args) {
        Main main = new Main();
        Engine gameEng = new Engine("Terraflat", new Window.WindowOptions(), main);
        gameEng.start();
    }

    @Override
    public void cleanup() {
        // Nothing to be done yet
    }

    @Override
    public void init(Window window, Scene scene, Render render) {
        Models.init(scene);

        Grid sun = Grids.createGrid("sun", new Grid(new Matrix4f().translate(0, 420, 0), 32));
        sun.addElement(new Element());
        Grid terrain = Grids.createGrid("terrain", new Grid(new Matrix4f().translate(0, 0, 0), 1));
        FastNoiseLite noise = new FastNoiseLite();
        noise.SetNoiseType(FastNoiseLite.NoiseType.Cellular);
        for (int x = -100; x <= 100; x++) {
            for (int z = -100; z <= 100; z++) {
                float baseCellularNoise = noise.GetNoise(x, z);
                for (int y = 128; y >= 0; y--) {
                    double baseGradient = TerraflatMath.gradient(y, 128, 0, 2, -1);
                    if (baseCellularNoise+baseGradient > 0) {
                        terrain.addElement(new Element(), new Vector3i(x, y, z));
                        y = -1;
                    }
                }
            }
        }
    }

    @Override
    public void input(Window window, Scene scene, long diffTimeMillis) {
        float move = diffTimeMillis * MOVEMENT_SPEED;
        Camera camera = scene.getCamera();
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

    @Override
    public void update(Window window, Scene scene, long diffTimeMillis) {
        Grids.getGrids().forEach((String name, Grid grid) -> {
            Matrix4f gridMatrix = grid.getMatrix();
            if (name.equals("sun")) {
                gridMatrix.rotateXYZ(new Vector3f(0.001f, 0.002f, 0.005f));
                gridMatrix.rotateLocalX(0.001f);
                grid.setMatrix(gridMatrix);
            }
            grid.getElements().forEach((Vector3i pos, Element element) -> {
                String entityId = name+pos.x+"/"+pos.y+"/"+pos.z;
                Entity entity = scene.getModelMap().get(element.getModel()).getEntitiesMap().get(entityId);
                boolean isNew = false;
                if (entity == null) {
                    isNew = true;
                    entity = new Entity(entityId, element.getModel());
                    entity.setScale(grid.getScale());
                }

                entity.setMatrix(new Matrix4f(gridMatrix).translate(pos.x*2, pos.y*2, pos.z*2).scale(entity.getScale()));

                if (isNew) {
                    scene.addEntity(entity);
                }
            });
        });
    }
}
