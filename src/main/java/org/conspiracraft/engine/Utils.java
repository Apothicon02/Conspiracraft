package org.conspiracraft.engine;

import org.joml.Vector2i;
import org.joml.Vector4i;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;

public class Utils {
    public static byte emptyByte = (byte)0;

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

    public static int packBools(BitSet set) {
        int value = 0;
        for (int i = 0; i < set.length(); i++) {
            if (set.get(i)) {
                value |= (1 << i);
            }
        }
        return value;
    }
    public static BitSet unpackBools(int packed) {
        BitSet set = new BitSet(32);
        for (int i = 0; i < 32; i++) {
            if ((packed & (1 << i)) != 0) {
                set.set(i);
            }
        }
        return set;
    }
    public static int pack16Ints(int[] values) {
        int packed = 0;
        for (int i = 0; i < values.length; i++) {
            packed |= (values[i] & 0x3) << (i * 2);
        }
        return packed;
    }
    public static int[] unpackPacked16Ints(int packed) {
        int[] values = new int[16];
        for (int i = 0; i < 16; i++) {
            values[i] = (packed >> (i*2)) & 0x3;
        }
        return values;
    }
    public static int pack4Ints(int one, int two, int three, int four) {
        return (one << 24) | (two << 16) | (three << 8) | four;
    }
    public static Vector4i unpackPacked4Ints(int packed) {
        return new Vector4i(0xFF & packed >> 24, 0xFF & packed >> 16, 0xFF & packed >> 8, 0xFF & packed);
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
}
