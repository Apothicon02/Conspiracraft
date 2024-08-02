package org.apothicon.core;

import org.apothicon.Main;
import org.apothicon.core.elements.Element;
import org.apothicon.core.elements.Mesh;
import org.apothicon.core.elements.Models;
import org.apothicon.core.grids.Grid;
import org.apothicon.core.grids.Grids;
import org.apothicon.core.rendering.Camera;
import org.apothicon.core.rendering.Renderer;
import org.apothicon.core.utilities.Constants;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL40;

public class Terraflat implements ILogic {
    private final Renderer renderer;
    private final WindowManager window;
    private Camera camera;
    Vector3f cameraInc;
    private boolean wireframeMode = false;
    int wireframeToggleCD = 20;


    public Terraflat() {
        renderer = new Renderer();
        window = Main.getWindow();
        camera = new Camera();
        cameraInc = new Vector3f(0, 0, 0);
    }

    @Override
    public void init() throws Exception {
        renderer.init();

        //model.setTexture(new Texture(loader.loadTexture(getClass().getResource("/textures/blocks/sun.png").getPath().toString().substring(1))));
        Grids.createGrid("sun", new Grid(new Vector3f(0, 1, 0), new Vector3f(45, 45, 45), 1)).addElement(new Element()); //10000 1000)
    }

    @Override
    public void input() {
        wireframeToggleCD--;
        if (window.isKeyPressed(GLFW.GLFW_KEY_F6) && wireframeToggleCD <= 0) {
            wireframeToggleCD = 20;
            if (wireframeMode) {
                GL40.glPolygonMode(GL40.GL_FRONT_AND_BACK, GL40.GL_FILL);
            } else {
                GL40.glPolygonMode(GL40.GL_FRONT_AND_BACK, GL40.GL_LINE);
            }
            wireframeMode = !wireframeMode;
        }

        cameraInc.set(0, 0, 0);
        if (window.isKeyPressed(GLFW.GLFW_KEY_W)) {
            cameraInc.z = -1;
        }
        if (window.isKeyPressed(GLFW.GLFW_KEY_S)) {
            cameraInc.z = 1;
        }

        if (window.isKeyPressed(GLFW.GLFW_KEY_A)) {
            cameraInc.x = -1;
        }
        if (window.isKeyPressed(GLFW.GLFW_KEY_D)) {
            cameraInc.x = 1;
        }

        if (window.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL)) {
            cameraInc.y = -1;
        }
        if (window.isKeyPressed(GLFW.GLFW_KEY_SPACE)) {
            cameraInc.y = 1;
        }
    }

    @Override
    public void update(float interval, MouseInput input) {
        camera.movePosition(cameraInc.x * Constants.CAMERA_MOVE_SPEED, cameraInc.y * Constants.CAMERA_MOVE_SPEED, cameraInc.z * Constants.CAMERA_MOVE_SPEED);

        if (input.isRmb()) {
            Vector2f rotVec = input.getDisplayVec();
            camera.moveRotation(rotVec.x * Constants.MOUSE_SENSITIVITY, rotVec.y * Constants.MOUSE_SENSITIVITY, 0);
        }
    }

    @Override
    public void render() {
        if (window.isResize()) {
            GL40.glViewport(0, 0, window.getWidth(), window.getHeight());
            window.setResize(true);
        }

        window.setClearColor(0, 0, 0, 0);
        Grids.getGrids().forEach((String name, Grid grid) -> {
            grid.getElements().forEach((Vector3i pos, Element element) -> {
                renderer.render(element, camera);
            });
        });
    }

    @Override
    public void cleanup() {
        renderer.cleanup();
    }
}
