package org.conspiracraft.renderer.assets;

import org.lwjgl.opengl.GL40;

import java.util.ArrayList;
import java.util.List;

public class Textures {
    public static int[] defaultParameters = new int[]{
            GL40.GL_TEXTURE_MIN_FILTER , GL40.GL_NEAREST,
            GL40.GL_TEXTURE_MAG_FILTER,GL40.GL_NEAREST,
            GL40.GL_TEXTURE_WRAP_S, GL40.GL_CLAMP_TO_EDGE,
            GL40.GL_TEXTURE_WRAP_T, GL40.GL_CLAMP_TO_EDGE,
            GL40.GL_TEXTURE_WRAP_R, GL40.GL_CLAMP_TO_EDGE};

    public static List<Texture> textures = new ArrayList<>(List.of());

    public static Texture create(int width, int height) {
        Texture texture = new Texture(defaultParameters, width, height);
        textures.addLast(texture);
        return texture;
    }
    public static Texture create(int[] params, int width, int height) {
        Texture texture = new Texture(params, width, height);
        textures.addLast(texture);
        return texture;
    }
    public static Texture create(int width, int height, int depth) {
        Texture texture = new Texture3D(defaultParameters, width, height, depth);
        textures.addLast(texture);
        return texture;
    }
    public static Texture create(int[] params, int width, int height, int depth) {
        Texture texture = new Texture3D(params, width, height, depth);
        textures.addLast(texture);
        return texture;
    }

    public static void generate() {
        textures.forEach((texture) -> {
            texture.id = GL40.glGenTextures();
            int textureType = texture instanceof Texture3D ? GL40.GL_TEXTURE_3D : GL40.GL_TEXTURE_2D;
            GL40.glBindTexture(textureType, texture.id);
            for (int i = 0; i < texture.parameters.length; i+=2) {
                GL40.glTexParameteri(textureType, texture.parameters[i], texture.parameters[i+1]);
            }
        });
    }
}
