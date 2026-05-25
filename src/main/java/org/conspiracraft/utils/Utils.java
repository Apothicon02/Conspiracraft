package org.conspiracraft.utils;

import org.conspiracraft.Main;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4i;
import sun.misc.Unsafe;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

import static org.conspiracraft.Main.mainFolder;
import static org.lwjgl.system.MemoryUtil.memAlloc;

public class Utils {
    public static Random random = new Random(67);
    public static float randomFloat(float mul) {return random.nextFloat()*mul;}

    @SuppressWarnings("removal")
    public static void unmap(MappedByteBuffer buffer) {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            unsafe.invokeCleaner(buffer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFile(String filePath) {
        List<String> file = new BufferedReader(new InputStreamReader(Utils.class.getClassLoader().getResourceAsStream(filePath))).lines().toList();
        StringBuilder data = new StringBuilder();
        for (String s : file) {
            data.append(s).append("\n");
        }
        return data.toString();
    }
    public static BufferedImage loadImage(String name) throws IOException {
        InputStream inputStream = Files.newInputStream(Paths.get(mainFolder + "assets/base/" + name + ".png"));
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
        ByteBuffer buffer = memAlloc(pixels.length * 4);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();
        return buffer;
    }
    public static void imageToBuffer(ByteBuffer buffer, int width, int height, BufferedImage image) {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        int padColumnsBytes = (width-image.getWidth())*4;
        for (int y = 0; y < image.getHeight(); y++) {
            int rowStart = y * image.getWidth();
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[rowStart+x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
            for (int i = 0; i < padColumnsBytes; i++) {
                buffer.put((byte)0);
            }
        }
        int widthBytes = width*4;
        int padRows = height-image.getHeight();
        for (int row =  0; row < padRows; row++) {
            for (int i = 0; i < widthBytes; i++) {
                buffer.put((byte) 0);
            }
        }
        buffer.flip();
    }
    public static long[] flipLongArray(long[] arr) {
        long[] newArr = new long[arr.length];
        int i = arr.length-1;
        for (long value : arr) {
            newArr[i--] = value;
        }
        return newArr;
    }
    public static int[] flipIntArray(int[] arr) {
        int[] newArr = new int[arr.length];
        int i = arr.length-1;
        for (int value : arr) {
            newArr[i--] = value;
        }
        return newArr;
    }
    public static byte[] longArrayToByteArray(long[] longArr) {
        byte[] byteArr = new byte[longArr.length*8];
        for (int i = 0; i < longArr.length; i++) {
            long val = longArr[i];
            byteArr[(i*8)] = (byte) (val >> 56);
            byteArr[(i*8)+1] = (byte) (val >> 48);
            byteArr[(i*8)+2] = (byte) (val >> 40);
            byteArr[(i*8)+3] = (byte) (val >> 32);
            byteArr[(i*8)+4] = (byte) (val >> 24);
            byteArr[(i*8)+5] = (byte) (val >> 16);
            byteArr[(i*8)+6] = (byte) (val >> 8);
            byteArr[(i*8)+7] = (byte) (val);
        }
        return byteArr;
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
    public static byte[] shortArrayToByteArray(short[] shortArr) {
        byte[] byteArr = new byte[shortArr.length*2];
        for (int i = 0; i < shortArr.length; i++) {
            byteArr[i*2] = (byte) (shortArr[i] >> 8);
            byteArr[(i*2)+1] = (byte)shortArr[i];
        }
        return byteArr;
    }
    public static short[] byteArrayToShortArray(short[] shortArr, byte[] byteArr) {
        for (int i = 0; i < shortArr.length; i++) {
            shortArr[i] = (short)(((byteArr[i*2] & 0xFF) << 8) | (byteArr[(i*2)+1] & 0xFF));
        }
        return shortArr;
    }
    public static short[] byteArrayToShortArray(byte[] byteArr) {
        return byteArrayToShortArray(new short[byteArr.length/2], byteArr);
    }
    public static long[] byteArrayToLongArray(long[] longArr, byte[] byteArr) {
        ByteBuffer buf = ByteBuffer.wrap(byteArr).order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < longArr.length; i++) {
            longArr[i] = buf.getLong();
        }
        return longArr;
    }
    public static long[] byteArrayToLongArray(byte[] byteArr) {
        return byteArrayToLongArray(new long[byteArr.length/8], byteArr);
    }
    public static int[] byteArrayToIntArray(byte[] byteArr) {
        int[] intArr = new int[byteArr.length/4];
        for (int i = 0; i < intArr.length; i++) {
            intArr[i] = ((byteArr[i*4] & 0xFF) << 24) | ((byteArr[(i*4)+1] & 0xFF) << 16) | ((byteArr[(i*4)+2] & 0xFF) << 8) | (byteArr[(i*4)+3] & 0xFF);
        }
        return intArr;
    }
    public static Vector3f unzeroVec(Vector3f dir) {
        if (dir.x() == 0.0f) {
            dir.x = 0.001f;
        }
        if (dir.y() == 0.0f) {
            dir.y = 0.001f;
        }
        if (dir.z() == 0.0f) {
            dir.z = 0.001f;
        }
        return dir;
    }
    public static float step(float edge, float f) {
        return f >= edge ? 1.f : 0.f;
    }
    public static Vector3f step(Vector3f edge, float f) {
        return new Vector3f(step(edge.x(), f), step(edge.y(), f), step(edge.z(), f));
    }
    public static float sign(float f) {
        float signum = Math.signum(f);
        return signum == -0.0f ? 0.0f : signum;
    }
    public static Vector3f sign(Vector3f vec) {
        return new Vector3f(sign(vec.x()), sign(vec.y()), sign(vec.z()));
    }
    public static float furthestFromZeroMix(float first, float second, float mix) {
        return mix(first, Math.abs(first) > Math.abs(second) ? first : second, mix);
    }
    public static float furthestFromZero(float first, float second) {
        return Math.abs(first) > Math.abs(second) ? first : second;
    }
    public static float mix(float min, float max, float factor) {
        return min * (1 - factor) + max * factor;
    }
    public static double mix(double min, double max, double factor) {
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

    public static Vector3i addVec(Vector3i vec, int x, int y, int z) {
        return new Vector3i(vec.x+x, vec.y+y, vec.z+z);
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
