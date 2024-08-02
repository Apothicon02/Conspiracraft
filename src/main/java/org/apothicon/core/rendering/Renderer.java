package org.apothicon.core.rendering;

import org.apothicon.Main;
import org.apothicon.core.WindowManager;
import org.apothicon.core.elements.Element;
import org.apothicon.core.elements.Mesh;
import org.apothicon.core.utilities.Transformation;
import org.apothicon.core.utilities.Utils;
import org.lwjgl.opengl.GL40;

public class Renderer {

    private final WindowManager window;
    private Shaders shader;

    public Renderer() {
        window = Main.getWindow();
    }

    public void init() throws Exception {
        shader = new Shaders();
        shader.createVertexShader(Utils.loadResource("/shaders/vertex.vsh"));
        shader.createFragmentShader(Utils.loadResource("/shaders/fragment.fsh"));
        shader.link();
        shader.createUniforms("texSampler");
        shader.createUniforms("transformationMatrix");
        shader.createUniforms("projectionMatrix");
        shader.createUniforms("viewMatrix");
    }

    public void render(Element element, Camera camera) {
        clear();
        shader.bind();
        shader.setUniform("texSampler", 0);
        shader.setUniform("transformationMatrix", Transformation.createTransformationMatrix(element));
        shader.setUniform("projectionMatrix", window.updateProjectionMatrix());
        shader.setUniform("viewMatrix", Transformation.getViewMatrix(camera));
        Mesh mesh = element.getMesh();
        GL40.glBindVertexArray(mesh.getVaoId());
        GL40.glEnableVertexAttribArray(0);
        GL40.glEnableVertexAttribArray(1);
        //GL40.glActiveTexture(GL40.GL_TEXTURE_2D);
        //GL40.glBindTexture(GL40.GL_TEXTURE_2D, mesh.getTexture().getId());
        GL40.glDrawElements(GL40.GL_TRIANGLES, mesh.getVertexCount(), GL40.GL_UNSIGNED_INT, 0); //this line is causing the crash somehow

        GL40.glDisableVertexAttribArray(0);
        GL40.glDisableVertexAttribArray(1);
        GL40.glBindVertexArray(0);
        shader.unbind();
    }

    public void clear() {
        GL40.glClear(GL40.GL_COLOR_BUFFER_BIT | GL40.GL_DEPTH_BUFFER_BIT);
    }

    public void cleanup() {
        shader.cleanup();
    }
}
