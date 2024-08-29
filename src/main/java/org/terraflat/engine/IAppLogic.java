package org.terraflat.engine;

import org.terraflat.engine.graph.Render;
import org.terraflat.engine.scene.Scene;

public interface IAppLogic {

    void cleanup();

    void init(Window window, Scene scene, Render render);

    void input(Window window, Scene scene, long diffTimeMillis);

    void update(Window window, Scene scene, long diffTimeMillis);
}