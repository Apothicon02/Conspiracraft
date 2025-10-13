package org.conspiracraft.game;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.conspiracraft.Main;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.conspiracraft.Main.gson;

public class Config {
    public static String configPath = Main.mainFolder+"settings.json";
    public static JsonObject config = null;

    public static void writeConfig() throws IOException {
        JsonWriter writer = new JsonWriter(new FileWriter(configPath));
        config = new JsonObject();
        config.addProperty("MOUSE_SENSITIVITY", Main.MOUSE_SENSITIVITY);
        config.addProperty("RENDER_DISTANCE", Renderer.renderDistanceMul);
        config.addProperty("SHADOWS_ENABLED", Renderer.shadowsEnabled);
        config.addProperty("REFLECTIONS_ENABLED", Renderer.reflectionsEnabled);
        config.addProperty("AO_QUALITY", Renderer.aoQuality);
        gson.toJson(config, writer);
        writer.close();
    }

    public static void readConfig() throws IOException {
        if (Files.exists(Path.of(configPath))) {
            JsonReader reader = new JsonReader(new FileReader(configPath));
            config = gson.fromJson(reader, JsonObject.class);
            Main.MOUSE_SENSITIVITY = config.get("MOUSE_SENSITIVITY").getAsFloat();
            try {Renderer.renderDistanceMul = config.get("RENDER_DISTANCE").getAsInt();} catch(NullPointerException exc) {}
            try {Renderer.shadowsEnabled = config.get("SHADOWS_ENABLED").getAsBoolean();} catch(NullPointerException exc) {}
            try {Renderer.reflectionsEnabled = config.get("REFLECTIONS_ENABLED").getAsBoolean();} catch(NullPointerException exc) {}
            try {Renderer.aoQuality = config.get("AO_QUALITY").getAsInt();} catch(NullPointerException exc) {}
            reader.close();
        } else {
            writeConfig();
        }
    }
}
