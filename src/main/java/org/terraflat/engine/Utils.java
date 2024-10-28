package org.terraflat.engine;

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
}
