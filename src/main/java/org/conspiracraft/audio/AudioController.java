package org.conspiracraft.audio;

import org.joml.Vector3f;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;
import org.conspiracraft.Main;
import org.conspiracraft.world.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.openal.ALC11.ALC_MONO_SOURCES;
import static org.lwjgl.openal.ALC11.ALC_STEREO_SOURCES;

public class AudioController {
    public static long context;
    public static long device;
    public static ALCCapabilities alcCapabilities;
    public static ALCapabilities alCapabilities;
    public static int outputMode = SOFTOutputMode.ALC_SURROUND_7_1_SOFT;
    public static boolean muted = false;
    public static float masterVolume = 1.f;
    public static Source hoverSource = null;
    public static Source buttonSource = null;
    public static Source sliderSource = null;

    public static void init() {
        //long audioInitStarted = System.currentTimeMillis();
        String defaultDeviceName = ALC11.alcGetString(0, ALC11.ALC_DEFAULT_DEVICE_SPECIFIER);
        device = ALC11.alcOpenDevice(defaultDeviceName);
        alcCapabilities = ALC.createCapabilities(device);

        context = ALC11.alcCreateContext(device, new int[]{ALC_MONO_SOURCES, 16256, ALC_STEREO_SOURCES, 128, SOFTOutputMode.ALC_OUTPUT_MODE_SOFT, AudioController.outputMode, 0});
        ALC11.alcMakeContextCurrent(context);

        alCapabilities = AL.createCapabilities(alcCapabilities);
        AL10.alDistanceModel(AL11.AL_EXPONENT_DISTANCE);

//        int maxSourcesBeforeError = 0;
//        while (true) {
//            AL10.alGenSources();
//            if (AL10.alGetError() != AL_NO_ERROR) {
//                break;
//            }
//            maxSourcesBeforeError++;
//        }
        AudioController.setListenerData(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new float[6]);
        hoverSource = new Source(new Vector3f(), 0.15f, 1.6f, 1.f, 0);
        buttonSource = new Source(new Vector3f(), 0.3f, 1.1f, 1.f, 0);
        sliderSource = new Source(new Vector3f(), 1.f, 1.1f, 1.f, 1);
        //System.out.print("Took "+String.format("%.2f", (System.currentTimeMillis()-audioInitStarted)/1000.f)+"s to init openAL.\n");
    }

    public static void playHoverSound() {
        hoverSource.play(Math.random() > 0.5f ? Sounds.ROCK_PLACE1 : Sounds.ROCK_PLACE2, true);
    }
    public static void playButtonSound() {
        buttonSource.play(Math.random() > 0.5f ? Sounds.ROCK_PLACE1 : Sounds.ROCK_PLACE2, true);
    }
    public static boolean playedSliderSound = false;
    public static float prevX = 0;
    public static void playSliderSound() {
        playedSliderSound = true;
        float x = Main.player.inputHandler.currentPos.x();
        float gain = (Math.abs(x-prevX))/2.f;
        if (gain > 0.f) {
            gain = Math.clamp(gain, 0.8f, 1.f);
            sliderSource.setGain(gain);
            sliderSource.play(Sounds.GEARS_TURNING, false);
        }
        prevX = x;
    }

    public static String getOutputModeAsTxt() {
        if (AudioController.outputMode == SOFTOutputMode.ALC_MONO_SOFT) {
            return "  Mono  ";
        } else if (AudioController.outputMode == SOFTOutputMode.ALC_STEREO_HRTF_SOFT) {
            return " Stereo ";
        } else if (AudioController.outputMode == SOFTOutputMode.ALC_SURROUND_5_1_SOFT) {
            return "  5.1  ";
        } else if (AudioController.outputMode == SOFTOutputMode.ALC_SURROUND_6_1_SOFT) {
            return "  6.1  ";
        } else if (AudioController.outputMode == SOFTOutputMode.ALC_SURROUND_7_1_SOFT) {
            return "  7.1  ";
        } else {
            return "Unknown";
        }
    }

    public static ArrayList<Source> disposableSources = new ArrayList<>(List.of());

    public static void tick() {
//        if (alGetError() != AL_NO_ERROR) {
//            boolean nothing = true;
//        }
        if (!playedSliderSound) {
            sliderSource.stop();
        }
        buttonSource.setPos(Main.player.pos);
        hoverSource.setPos(Main.player.pos);
        sliderSource.setPos(Main.player.pos);
        for (int i = 0; i < disposableSources.size(); i++) {
            Source source = disposableSources.get(i);
            if (!source.isPlaying()) {
                source.delete();
                source = null;
                disposableSources.remove(i--);
            }
        }
        playedSliderSound = false;
    }

    public static void setListenerData(Vector3f pos, Vector3f vel, float[] orientation) {
        AL10.alListener3f(AL10.AL_POSITION, pos.x, pos.y, pos.z);
        AL10.alListener3f(AL10.AL_VELOCITY, vel.x, vel.y, vel.z);
        AL10.alListenerfv(AL10.AL_ORIENTATION, orientation);
        if (muted) {
            AL10.alListenerf(AL10.AL_GAIN, 0.f);
        } else {
            AL10.alListenerf(AL10.AL_GAIN, masterVolume);
        }
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
        String folder = Main.resourcesPath+path;
        Path folderPath = Path.of(folder);
        if (Files.exists(folderPath)) {
            int buffer = AL10.alGenBuffers();
            List<String> allSounds = new ArrayList<>(Arrays.asList(new File(folder).list()));
            if (allSounds.size() > 1) {
                allSounds.remove(prevRandomSound);
                String name = allSounds.get((int) (Math.random() * (allSounds.size() - 1)));
                prevRandomSound = name;
                WaveData waveFile = WaveData.createFromAppdata(folder + name);
                AL10.alBufferData(buffer, waveFile.format, waveFile.data, waveFile.sampleRate);
                waveFile.dispose();
                return new SFX(buffer, (float) (waveFile.totalBytes / waveFile.bytesPerFrame) / waveFile.sampleRate);
            }
        } else {
            Files.createDirectory(folderPath);
        }
        return Sounds.CLOUD;
    }

    public static void cleanup() {
        ALC11.alcMakeContextCurrent(MemoryUtil.NULL);
        ALC11.alcDestroyContext(context);
        ALC11.alcCloseDevice(device);
    }
}
