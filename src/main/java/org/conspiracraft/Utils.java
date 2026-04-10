package org.conspiracraft;

import org.joml.Vector3f;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;

public class Utils {
    public static String readFile(String filePath) {
        List<String> file = new BufferedReader(new InputStreamReader(Utils.class.getClassLoader().getResourceAsStream(filePath))).lines().toList();
        StringBuilder data = new StringBuilder();
        for (String s : file) {
            data.append(s).append("\n");
        }
        return data.toString();
    }
    public static BufferedImage loadImage(String name) throws IOException {
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("assets/base/"+name+".png");
        BufferedInputStream bInputStream = new BufferedInputStream(inputStream);
        ImageReader reader = ImageIO.getImageReadersByFormatName("png").next();
        reader.setInput(ImageIO.createImageInputStream(bInputStream), true);
        BufferedImage image = reader.read(0);
        inputStream.close();
        bInputStream.close();
        return image;
    }

    public static ByteBuffer imageToBuffer(BufferedImage image) {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        ByteBuffer buffer = ByteBuffer.allocateDirect(pixels.length * 4);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();
        return buffer;
    }
    public static float mix(float min, float max, float factor) {
        return min * (1 - factor) + max * factor;
    }
    public static Vector3f getInterpolatedVec(Vector3f old, Vector3f current) {
        return new Vector3f(mix(old.x, current.x, (float) Main.interpolationTime), mix(old.y, current.y, (float) Main.interpolationTime), mix(old.z, current.z, (float) Main.interpolationTime));
    }
    public static float getInterpolatedFloat(float old, float current) {
        return mix(old, current, (float) Main.interpolationTime);
    }
    public static double averageLongs(List<Long> numbers) {
        double sum = 0.0;
        for (double num : numbers) {
            sum += num;
        }
        return sum / numbers.size();
    }

    public static double gradient(int y, int fromY, int toY, float toValue, float fromValue) {
        return clampedLerp(fromValue, toValue, inverseLerp(y, fromY, toY));
    }
    public static double inverseLerp(double y, double fromY, double toY) {
        return (y - fromY) / (toY - fromY);
    }
    public static double clampedLerp(double toValue, double fromValue, double invLerpValue) {
        if (invLerpValue < 0.0D) {
            return toValue;
        } else {
            return invLerpValue > 1.0D ? fromValue : lerp(invLerpValue, toValue, fromValue);
        }
    }
    public static double lerp(double invLerpValue, double toValue, double fromValue) {
        return toValue + invLerpValue * (fromValue - toValue);
    }
}
