package org.conspiracraft.game.audio;

import org.joml.Vector3f;
import org.lwjgl.openal.AL10;

public class Source {
    private int sourceID;
    public int soundPlaying = -1;
    float baseGain;
    float basePitch;
    float threshold = 0;

    public Source(Vector3f pos, float gain, float pitch, float threshold) {
        this.threshold = threshold;
        sourceID = AL10.alGenSources();
        AL10.alSourcef(sourceID, AL10.AL_GAIN, gain);
        AL10.alSourcef(sourceID, AL10.AL_PITCH, pitch);
        baseGain = gain;
        basePitch = pitch;
        AL10.alSource3f(sourceID, AL10.AL_POSITION, pos.x, pos.y, pos.z);
    }

    public void play(SFX sfx) {
        int[] result = new int[1];
        AL10.alGetSourcei(sourceID, AL10.AL_SOURCE_STATE, result);
        if (result[0] != AL10.AL_PLAYING) {
            soundPlaying = sfx.id;
            AL10.alSourcei(sourceID, AL10.AL_BUFFER, sfx.id);
            AL10.alSourcePlay(sourceID);
        }
    }

    public void stop() {
        soundPlaying = -1;
        AL10.alSourceStop(sourceID);
    }

    public void setPos(Vector3f pos) {
        AL10.alSource3f(sourceID, AL10.AL_POSITION, pos.x, pos.y, pos.z);
    }
    public void setVel(Vector3f vel) {
        AL10.alSource3f(sourceID, AL10.AL_VELOCITY, vel.x, vel.y, vel.z);
        float speed = Math.max(threshold, Math.max(Math.abs(vel.x), Math.max(Math.abs(vel.y), Math.abs(vel.z))))-threshold;
        if (baseGain < 0) {
            AL10.alSourcef(sourceID, AL10.AL_GAIN, speed*Math.abs(baseGain));
        } else {
            AL10.alSourcef(sourceID, AL10.AL_GAIN, (baseGain / 2) + (speed * (baseGain / 2)));
        }
        if (basePitch < 0) {
            AL10.alSourcef(sourceID, AL10.AL_PITCH, speed*Math.abs(basePitch));
        } else {
            AL10.alSourcef(sourceID, AL10.AL_PITCH, basePitch+(speed/10));
        }
    }

    public void delete() {
        AL10.alDeleteSources(sourceID);
    }
}