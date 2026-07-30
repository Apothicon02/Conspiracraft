package org.conspiracraft.gui;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import org.conspiracraft.Main;
import org.conspiracraft.Settings;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Languages {
    public static final List<Map<String, JsonElement>> languages = new ArrayList<>();
    public static void load() throws IOException {
        try (Stream<Path> pathStream = Files.walk(Paths.get(Main.mainFolder+"assets/base/language/"))) {
            List<Path> paths = pathStream.filter(Files::isRegularFile).sorted().toList();
            for (Path path : paths) {
                JsonReader reader = new JsonReader(new FileReader(path.toString()));
                JsonObject data = new Gson().fromJson(reader, JsonObject.class);
                if (path.endsWith("american.json")) {
                    languages.addFirst(data.asMap());
                } else {
                    languages.addLast(data.asMap());
                }
            }
        }
    }
    public static String translate(String key) {
        JsonElement value = languages.get(Settings.language).get(key);
        if (value == null) {value = languages.getFirst().get(key);}
        return value == null ? key : value.getAsString();
    }
}
