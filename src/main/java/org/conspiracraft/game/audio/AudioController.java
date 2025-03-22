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

    public static List<SFX> buffers = new ArrayList<>();

    public static void init() {
        String defaultDeviceName = ALC10.alcGetString(0, ALC10.ALC_DEFAULT_DEVICE_SPECIFIER);
        device = ALC10.alcOpenDevice(defaultDeviceName);
        alcCapabilities = ALC.createCapabilities(device);

        context = ALC10.alcCreateContext(device, (IntBuffer) null);
        ALC10.alcMakeContextCurrent(context);

        alCapabilities = AL.createCapabilities(alcCapabilities);
    }

    public static void setListenerData(Vector3f pos, Vector3f vel, float[] orientation) {
        AL10.alListener3f(AL10.AL_POSITION, pos.x, pos.y, pos.z);
        AL10.alListener3f(AL10.AL_VELOCITY, vel.x, vel.y, vel.z);
        AL10.alListenerfv(AL10.AL_ORIENTATION, orientation);
    }

    public static void loadSound(String file) {
        int buffer = AL10.alGenBuffers();
        WaveData waveFile = WaveData.create(file);
        buffers.add(new SFX(buffer, (float) (waveFile.totalBytes / waveFile.bytesPerFrame) / waveFile.sampleRate));
        AL10.alBufferData(buffer, waveFile.format, waveFile.data, waveFile.sampleRate);
        waveFile.dispose();
    }

    public static String resourcesPath = System.getenv("APPDATA")+"/Conspiracraft/resources/";

    public static void loadRandomSound(String path) throws FileNotFoundException {
        String folder = resourcesPath+path;
        if (Files.exists(Path.of(folder))) {
            int buffer = AL10.alGenBuffers();
            List<String> allSounds = new ArrayList<>(Arrays.asList(new File(folder).list()));
            String name = allSounds.get((int) (Math.random() * (allSounds.size() - 1)));
            WaveData waveFile = WaveData.createFromAppdata(folder + name);
            buffers.add(new SFX(buffer, (float) (waveFile.totalBytes / waveFile.bytesPerFrame) / waveFile.sampleRate));
            AL10.alBufferData(buffer, waveFile.format, waveFile.data, waveFile.sampleRate);
            waveFile.dispose();
        } else {
            loadSound("grass_step1.wav");
        }
    }

    public static void cleanup() {
        for (SFX buffer : buffers) {
            AL10.alDeleteBuffers(buffer.id);
        }
        ALC10.alcMakeContextCurrent(MemoryUtil.NULL);
        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(device);
    }
}
