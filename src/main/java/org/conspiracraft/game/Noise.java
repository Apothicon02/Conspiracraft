package org.conspiracraft.game;

import org.conspiracraft.game.rendering.Renderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Noise {
    public static BufferedImage COHERERENT_NOISE;
    public static BufferedImage CELLULAR_NOISE;
    public static BufferedImage WHITE_NOISE;
    public static BufferedImage NOODLE_NOISE;
    public static BufferedImage CLOUD_NOISE;

    public static void init() throws IOException {
        COHERERENT_NOISE = loadImage("coherent_noise");
        CELLULAR_NOISE = loadImage("cellular_noise");
        WHITE_NOISE = loadImage("white_noise");
        NOODLE_NOISE = loadImage("noodle_noise");
        CLOUD_NOISE = loadImage("cloud_noise");
    }

    private static BufferedImage loadImage(String name) throws IOException {
        return ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/"+name+".png"));
    }

    public static float blue(int rgb) {
        return rgb & 0xff;
    }
}