package org.conspiracraft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.nio.file.*;

import static org.conspiracraft.audio.AudioController.*;

public class Settings {
    public static float mouseSensitivity = 0.1f;
    public static int width = 1920;
    public static int height = 1080;
    public static float fov = 73;
    public static int targetFps = 420;
    public static boolean shadowsEnabled = true;
    public static boolean reflectionsEnabled = true;
    public static boolean taaEnabled = true;
    public static boolean upscaleEnabled = true;
    public static boolean dynamicFoVEnabled = true;

    public static void load() throws IOException {
        Path path = Path.of(Main.mainFolder + "settings.json");
        Gson gson = new Gson();
        if (!Files.exists(path)) {
            JsonWriter writer = new JsonWriter(new FileWriter(path.toString()));
            JsonObject defaultData = gson.fromJson("{}", JsonObject.class);
            gson.toJson(defaultData, writer);
            writer.close();
        }
        JsonReader reader = new JsonReader(new FileReader(path.toString()));
        JsonObject data = gson.fromJson(reader, JsonObject.class);
        if (data.get("mouseSensitivity") != null) {
            mouseSensitivity = data.get("mouseSensitivity").getAsFloat();
        } else {
            data.addProperty("mouseSensitivity", mouseSensitivity);
        }
        if (data.get("fov") != null) {
            fov = data.get("fov").getAsFloat();
        } else {
            data.addProperty("fov", fov);
        }
        if (data.get("dynamicFoVEnabled") != null) {
            dynamicFoVEnabled = data.get("dynamicFoVEnabled").getAsBoolean();
        } else {
            data.addProperty("dynamicFoVEnabled", dynamicFoVEnabled);
        }
        if (data.get("shadowsEnabled") != null) {
            shadowsEnabled = data.get("shadowsEnabled").getAsBoolean();
        } else {
            data.addProperty("shadowsEnabled", shadowsEnabled);
        }
        if (data.get("reflectionsEnabled") != null) {
            reflectionsEnabled = data.get("reflectionsEnabled").getAsBoolean();
        } else {
            data.addProperty("reflectionsEnabled", reflectionsEnabled);
        }
        if (data.get("taaEnabled") != null) {
            taaEnabled = data.get("taaEnabled").getAsBoolean();
        } else {
            data.addProperty("taaEnabled", taaEnabled);
        }
        if (data.get("upscaleEnabled") != null) {
            upscaleEnabled = data.get("upscaleEnabled").getAsBoolean();
        } else {
            data.addProperty("upscaleEnabled", upscaleEnabled);
        }
        if (data.get("outputMode") != null) {
            outputMode = data.get("outputMode").getAsInt();
        } else {
            data.addProperty("outputMode", outputMode);
        }
        if (data.get("muted") != null) {
            muted = data.get("muted").getAsBoolean();
        } else {
            data.addProperty("muted", muted);
        }
        if (data.get("masterVolume") != null) {
            masterVolume = data.get("masterVolume").getAsFloat();
        } else {
            data.addProperty("masterVolume", masterVolume);
        }
        JsonWriter writer = new JsonWriter(new FileWriter(path.toString()));
        gson.toJson(data, writer);
        writer.close();
    }
    public static void save() throws IOException {
        Path path = Path.of(Main.mainFolder + "settings.json");
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(path.toString()));
        JsonObject data = gson.fromJson(reader, JsonObject.class);
        data.addProperty("mouseSensitivity", mouseSensitivity);
        data.addProperty("fov", fov);
        data.addProperty("dynamicFoVEnabled", dynamicFoVEnabled);
        data.addProperty("shadowsEnabled", shadowsEnabled);
        data.addProperty("reflectionsEnabled", reflectionsEnabled);
        data.addProperty("taaEnabled", taaEnabled);
        data.addProperty("upscaleEnabled", upscaleEnabled);
        data.addProperty("outputMode", outputMode);
        data.addProperty("muted", muted);
        data.addProperty("masterVolume", masterVolume);
        JsonWriter writer = new JsonWriter(new FileWriter(path.toString()));
        gson.toJson(data, writer);
        writer.close();
    }
}
