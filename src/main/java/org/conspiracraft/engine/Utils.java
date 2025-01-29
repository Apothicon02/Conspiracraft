package org.conspiracraft.engine;

import org.conspiracraft.game.world.World;
import org.joml.Vector2i;
import org.joml.Vector3i;
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

    public static int condensePos(int x, int z) {
        return (x * World.size) + z;
    }
    public static int condensePos(int x, int y, int z) {
        return (((x*World.size)+z)*World.height)+y;
    }
    public static int condensePos(int x, int y, int z, int customSize) {
        return (((x*customSize)+z)*World.height)+y;
    }
    public static int condensePos(Vector3i pos) {
        return (((pos.x*World.size)+pos.z)*World.height)+pos.y;
    }
    public static int condenseLocalPos(int x, int y, int z) {
        return (((x*World.chunkSize)+z)*World.chunkSize)+y;
    }
    public static int condenseLocalPos(Vector3i pos) {
        return (((pos.x*World.chunkSize)+pos.z)*World.chunkSize)+pos.y;
    }
    public static int condenseChunkPos(Vector3i pos) {
        return (((pos.x*World.sizeChunks)+pos.z)*World.heightChunks)+pos.y;
    }
    public static int condenseChunkPos(int x, int y, int z) {
        return (((x*World.sizeChunks)+z)*World.heightChunks)+y;
    }
    public static int condenseChunkPos(int x, int z) {
        return (x*World.sizeChunks)+z;
    }
}
