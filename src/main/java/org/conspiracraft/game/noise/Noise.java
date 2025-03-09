package org.conspiracraft.game.noise;

import java.awt.image.BufferedImage;

public class Noise {
    public BufferedImage image;
    public float[] data;
    public int width;
    public int height;

    public Noise(BufferedImage bufferedImage) {
        image = bufferedImage;
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
        data = new float[width*height];
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                data[(x*width)+z] = ((float)(image.getRGB(x, z) & 0xff)/128)-1;
            }
        }
    }

    public float sample(int x, int z) {
        int loopedX = x-((int)(x/width)*width);
        int loopedZ = z-((int)(z/height)*height);
        return data[(loopedX*width)+loopedZ];
    }
}
