package org.conspiracraft.engine;

import org.conspiracraft.game.blocks.Light;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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

    public static int packInts(int first4, int last4) {
        return (first4 << 16) | last4;
    }

    public static int colorToInt(Color color) {
        return color.getRed() << 16 | color.getGreen() << 8 | color.getBlue() | color.getAlpha() << 24;
    }
    public static int lightToInt(Light light) {
        return light.r() << 16 | light.g() << 8 | light.b() | light.s() << 24;
    }
}
