package org.conspiracraft.game.noise;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class Noise {
    public float[] data;
    public int width;
    public int height;

    public Noise(BufferedImage bufferedImage) {
        BufferedImage image = bufferedImage;
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
        data = new float[width*height];
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                data[(x*width)+z] = ((float)(image.getRGB(x, z) & 0xff)/128)-1;
            }
        }
    }

    public ByteBuffer byteData() {
        ByteBuffer byteData = ByteBuffer.allocateDirect(data.length);
        for (float oldData : data) {
            byteData.put((byte)((oldData+1)*128));
        }
        byteData.flip();
        return byteData;
    }

    public float sample(int x, int z) {
        int loopedX = x-((int)(x/width)*width);
        int loopedZ = z-((int)(z/height)*height);
        return data[(loopedX*width)+loopedZ];
    }
}
