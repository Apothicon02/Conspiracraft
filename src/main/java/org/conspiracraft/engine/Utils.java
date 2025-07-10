package org.conspiracraft.engine;

import org.conspiracraft.Main;
import org.conspiracraft.game.ConspiraMath;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class Utils {

    public static float furthestFromZero(float first, float second) {
        return Math.abs(first) > Math.abs(second) ? first : second;
    }

    public static Vector3f getInterpolatedVec(Vector3f old, Vector3f current) {
        return new Vector3f(ConspiraMath.mix(old.x, current.x, (float) Main.interpolationTime), ConspiraMath.mix(old.y, current.y, (float) Main.interpolationTime), ConspiraMath.mix(old.z, current.z, (float) Main.interpolationTime));
    }

    public static String readFile(String filePath) {
        List<String> file = new BufferedReader(new InputStreamReader(Utils.class.getClassLoader().getResourceAsStream(filePath))).lines().toList();
        StringBuilder data = new StringBuilder();
        for (String s : file) {
            data.append(s).append("\n");
        }
        return data.toString();
    }

    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static Vector2i unpackInt(int all8) {
        return new Vector2i(all8 >> 16, all8 & 0xFFFF);
    }


    public static int colorToInt(Color color) {
        return color.getRed() << 16 | color.getGreen() << 8 | color.getBlue() | color.getAlpha() << 24;
    }
}
