package org.conspiracraft.game;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Noise {
    public static BufferedImage COHERERENT_NOISE;
    public static BufferedImage CELLULAR_NOISE;
    public static BufferedImage WHITE_NOISE;

    public static void init() throws IOException {
        COHERERENT_NOISE = loadImage("coherent_noise");
        CELLULAR_NOISE = loadImage("cellular_noise");
        WHITE_NOISE = loadImage("white_noise");
    }

    private static BufferedImage loadImage(String name) throws IOException {
        return ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/"+name+".png"));
    }

    public static float blue(int rgb) {
        return rgb & 0xff;
    }
}