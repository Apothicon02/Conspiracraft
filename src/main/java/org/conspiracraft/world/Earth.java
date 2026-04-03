package org.conspiracraft.world;

import org.conspiracraft.Utils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.util.Random;

import static org.conspiracraft.Main.*;
import static org.conspiracraft.renderer.Renderer.drawCube;

public class Earth extends WorldType {
    public static Random seededRand = new Random(35311350L);
    public Random rand() {
        return seededRand;
    }
    public static Vector3f prevSunPos = new Vector3f(0, World.height*2, 0);
    public static Vector3f sunPos = new Vector3f(0, World.height*2, 0);
    public static Vector3f prevMunPos = new Vector3f(0, World.height*-2, 0);
    public static Vector3f munPos = new Vector3f(0, World.height*-2, 0);
    @Override
    public void renderCelestialBodies(MemoryStack stack) {
        Matrix4f sunMatrix = new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(Utils.getInterpolatedVec(prevSunPos, sunPos)).scale(120);
        Vector4f sunColor = new Vector4f(1.25f, 1.2f, 0, 1);
        drawCube(stack, sunMatrix, sunColor);
        Matrix4f munMatrix = new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(Utils.getInterpolatedVec(prevMunPos, munPos)).scale(40);
        Vector4f munColor = new Vector4f(0.9f, 0.88f, 1.f, 1);
        drawCube(stack, munMatrix, munColor);
    }
    @Override
    public Vector3f getSunPos() {return sunPos;}
    @Override
    public void tick() {
        prevSunPos.set(sunPos);
        sunPos.set(0, World.size*2, 0);
        sunPos.rotateZ(timeNs/1000000000.f);
        sunPos.rotateX(0.5f);
        sunPos.set(sunPos.x+(World.size/2f), sunPos.y, sunPos.z+(World.size/2f)+128);
        prevMunPos.set(munPos);
        munPos.set(0, World.size*-2, 0);
        munPos.rotateZ(timeNs/1000000000.f);
        sunPos.rotateX(-0.2f);
        munPos.set(munPos.x+(World.size/2f), munPos.y, munPos.z+(World.size/2f)+128);
    }
}
