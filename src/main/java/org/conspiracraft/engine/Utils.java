package org.conspiracraft.engine;

import org.conspiracraft.Main;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.List;

public class Utils {
    public static int[] flipIntArray(int[] arr) {
        int[] newArr = new int[arr.length];
        int i = arr.length-1;
        for (int integer : arr) {
            newArr[i--] = integer;
        }
        return newArr;
    }
    public static byte[] intArrayToByteArray(int[] intArr) {
        byte[] byteArr = new byte[intArr.length*4];
        int index = intArr.length;
        for (int i = 0; i < intArr.length; i++) {
            index--;
            int val = intArr[index];
            byteArr[(i*4)] = (byte) (val >> 24);
            byteArr[(i*4)+1] = (byte) (val >> 16);
            byteArr[(i*4)+2] = (byte) (val >> 8);
            byteArr[(i*4)+3] = (byte) (val);
        }
        return byteArr;
    }
    public static short[] intArrayToShortArray(int[] intArr) {
        short[] shortArr = new short[intArr.length*2];
        for (int i = 0; i < intArr.length; i++) {
            Vector2i data = unpackInt(intArr[i]);
            shortArr[(i*2)+1] = (short) data.x;
            shortArr[i*2] = (short) data.y;
        }
        return shortArr;
    }
    public static int[] shortArrayToIntArray(short[] shortArr) {
        int[] intArr = new int[shortArr.length/2];
        int index = intArr.length;
        for (int i = 0; i < intArr.length; i++) {
            index--;
            intArr[i] = packInts(shortArr[index*2], shortArr[(index*2)+1]);
        }
        return intArr;
    }
    public static int[] byteArrayToIntArray(byte[] byteArr) {
        int[] intArr = new int[byteArr.length/4];
        for (int i = 0; i < intArr.length; i++) {
            intArr[i] = ((byteArr[i*4] & 0xFF) << 24) | ((byteArr[(i*4)+1] & 0xFF) << 16) | ((byteArr[(i*4)+2] & 0xFF) << 8) | (byteArr[(i*4)+3] & 0xFF);
        }
        return intArr;
    }

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

    public static ByteBuffer imageToGrayscaleBuffer(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        ByteBuffer buffer = ByteBuffer.allocateDirect(pixels.length);
        for (int pixel : pixels) {
            buffer.put((byte) (pixel & 0xff));
        }
        buffer.flip();
        return buffer;
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static float furthestFromZero(float first, float second) {
        return Math.abs(first) > Math.abs(second) ? first : second;
    }

    public static Vector3f getInterpolatedVec(Vector3f old, Vector3f current) {
        return new Vector3f(ConspiracraftMath.mix(old.x, current.x, (float) Main.interpolationTime), ConspiracraftMath.mix(old.y, current.y, (float) Main.interpolationTime), ConspiracraftMath.mix(old.z, current.z, (float) Main.interpolationTime));
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
    public static int packColor(Vector4i color) {
        return color.x() << 16 | color.y() << 8 | color.z() | color.w() << 24;
    }
    public static Vector4i unpackColor(int color) {
        return new Vector4i(0xFF & color >> 16, 0xFF & color >> 8, 0xFF & color, 0xFF & color >> 24);
    }
}
