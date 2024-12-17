package org.conspiracraft.engine;

import org.joml.Vector2i;
import org.joml.Vector4i;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.System.in;

public class Utils {
    public static byte emptyByte = (byte)0;
    public static String readFile(String filePath) {
        List<String> file = new BufferedReader(new InputStreamReader(Utils.class.getClassLoader().getResourceAsStream(filePath))).lines().toList();
        StringBuilder data = new StringBuilder();
        for (String s : file) {
            data.append(s).append("\n");
        }
        return data.toString();
    }

    public static ByteBuffer imageToBuffer(BufferedImage image) {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        ByteBuffer buffer = BufferUtils.createByteBuffer(pixels.length * 4);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();
        return buffer;
    }

    public static int packInts(int first4, int last4) {
        return (first4 << 16) | last4;
    }
    public static Vector2i unpackInt(int all8) {
        return new Vector2i(all8 >> 16, all8 & 0xFFFF);
    }

    public static byte convertBoolArrayToByte(boolean[] source)
    {
        byte result = 0;
        int index = 8 - source.length;

        for (boolean b : source) {
            if (b) {
                result |= (byte) (1 << (7 - index));
            }
            index++;
        }

        return result;
    }

    public static boolean[] convertByteToBoolArray(byte b)
    {
        boolean[] result = new boolean[8];

        for (int i = 0; i < 8; i++) {
            result[i] = (b & (1 << i)) != 0;
        }

        return new boolean[]{result[7], result[6], result[5], result[4], result[3], result[2], result[1], result[0]}; //return reversed version
    }

    public static int colorToInt(Color color) {
        return color.getRed() << 16 | color.getGreen() << 8 | color.getBlue() | color.getAlpha() << 24;
    }
    public static int vector4iToInt(Vector4i vector4i) {
        return vector4i.x() << 16 | vector4i.y() << 8 | vector4i.z() | vector4i.w() << 24;
    }
}
