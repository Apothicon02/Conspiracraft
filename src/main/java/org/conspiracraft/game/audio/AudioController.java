package org.conspiracraft.game.audio;

import org.conspiracraft.engine.Engine;
import org.joml.Vector3f;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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

    public static ArrayList<Source> disposableSources = new ArrayList<>(List.of());

    public static void disposeSources() {
        disposableSources.forEach((Source source) -> {
            if (!source.isPlaying()) {
                source.delete();
            }
        });
    }

    public static void setListenerData(Vector3f pos, Vector3f vel, float[] orientation) {
        AL10.alListener3f(AL10.AL_POSITION, pos.x, pos.y, pos.z);
        AL10.alListener3f(AL10.AL_VELOCITY, vel.x, vel.y, vel.z);
        AL10.alListenerfv(AL10.AL_ORIENTATION, orientation);
    }

    public static SFX loadSound(String file) {
        int buffer = AL10.alGenBuffers();
        WaveData waveFile = WaveData.create(file);
        if (waveFile != null) {
            AL10.alBufferData(buffer, waveFile.format, waveFile.data, waveFile.sampleRate);
            waveFile.dispose();
            return new SFX(buffer, (float) (waveFile.totalBytes / waveFile.bytesPerFrame) / waveFile.sampleRate);
        }
        return new SFX(buffer, 0);
    }

    public static String prevRandomSound = "";
    public static SFX loadRandomSound(String path) throws IOException {
        String folder = Engine.resourcesPath+path;
        Path folderPath = Path.of(folder);
        if (Files.exists(folderPath)) {
            int buffer = AL10.alGenBuffers();
            List<String> allSounds = new ArrayList<>(Arrays.asList(new File(folder).list()));
            allSounds.remove(prevRandomSound);
            String name = allSounds.get((int) (Math.random() * (allSounds.size() - 1)));
            prevRandomSound = name;
            WaveData waveFile = WaveData.createFromAppdata(folder + name);
            AL10.alBufferData(buffer, waveFile.format, waveFile.data, waveFile.sampleRate);
            waveFile.dispose();
            return new SFX(buffer, (float) (waveFile.totalBytes / waveFile.bytesPerFrame) / waveFile.sampleRate);
        } else {
            Files.createDirectory(folderPath);
            return null;
        }
    }

    public static void cleanup() {
        ALC10.alcMakeContextCurrent(MemoryUtil.NULL);
        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(device);
    }
}
