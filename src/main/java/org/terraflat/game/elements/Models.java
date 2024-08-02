package org.terraflat.game.elements;

import org.terraflat.engine.graph.Model;
import org.terraflat.engine.scene.ModelLoader;
import org.terraflat.engine.scene.Scene;

public class Models {
    public static Model CUBE;

    public static void init(Scene scene) {
        CUBE = ModelLoader.loadModel("cube", "resources/models/cube/cube.obj",
                scene.getTextureCache());

        scene.addModel(CUBE);
    }
}