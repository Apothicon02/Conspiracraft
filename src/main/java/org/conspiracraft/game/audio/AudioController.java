package org.conspiracraft.game.audio;

import org.joml.Vector3f;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioController {
    public static long context;
    public static long device;
    public static ALCCapabilities alcCapabilities;
    public static ALCapabilities alCapabilities;

    public static void init() {
        String defaultDeviceName = ALC10.alcGetString(0, ALC10.ALC_DEFAULT_DEVICE_SPECIFIER);
        device = ALC10.alcOpenDevice(defaultDeviceName);
        alcCapabilities = ALC.createCapabilities(device);

        context = ALC10.alcCreateContext(device, (IntBuffer) null);
        ALC10.alcMakeContextCurrent(context);

        alCapabilities = AL.createCapabilities(alcCapabilities);
        AL10.alDistanceModel(AL11.AL_EXPONENT_DISTANCE);
    }

    public static void setListenerData(Vector3f pos, Vector3f vel, float[] orientation) {
        AL10.alListener3f(AL10.AL_POSITION, pos.x, pos.y, pos.z);
        AL10.alListener3f(AL10.AL_VELOCITY, vel.x, vel.y, vel.z);
        AL10.alListenerfv(AL10.AL_ORIENTATION, orientation);
    }

    public static String resourcesPath = System.getenv("APPDATA")+"/Conspiracraft/resources/";

    public static SFX loadSound(String file) {
        int buffer = AL10.alGenBuffers();
        WaveData waveFile = null;
        try {
            waveFile = WaveData.createFromAppdata(resourcesPath+"sounds/"+file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        AL10.alBufferData(buffer, waveFile.format, waveFile.data, waveFile.sampleRate);
        waveFile.dispose();
        return new SFX(buffer, (float) (waveFile.totalBytes / waveFile.bytesPerFrame) / waveFile.sampleRate);
    }

    public static SFX loadRandomSound(String path) throws FileNotFoundException {
        String folder = resourcesPath+path;
        if (Files.exists(Path.of(folder))) {
            int buffer = AL10.alGenBuffers();
            List<String> allSounds = new ArrayList<>(Arrays.asList(new File(folder).list()));
            String name = allSounds.get((int) (Math.random() * (allSounds.size() - 1)));
            WaveData waveFile = WaveData.createFromAppdata(folder + name);
            AL10.alBufferData(buffer, waveFile.format, waveFile.data, waveFile.sampleRate);
            waveFile.dispose();
            return new SFX(buffer, (float) (waveFile.totalBytes / waveFile.bytesPerFrame) / waveFile.sampleRate);
        } else {
            return loadSound("cloud.wav");
        }
    }

    public static void cleanup() {
        ALC10.alcMakeContextCurrent(MemoryUtil.NULL);
        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(device);
    }
}
