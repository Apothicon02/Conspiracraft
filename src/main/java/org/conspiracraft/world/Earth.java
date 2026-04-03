package org.conspiracraft.world;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.util.Random;

import static org.conspiracraft.renderer.Renderer.drawCube;

public class Earth extends WorldType {
    public static Random seededRand = new Random(35311350L);
    public Random rand() {
        return seededRand;
    }
    public static Vector3f munPos = new Vector3f(0, World.height*-2, 0);
    @Override
    public void renderCelestialBodies(MemoryStack stack) {
        Matrix4f sunMatrix = new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(World.sunPos).scale(60);
        Vector4f sunColor = new Vector4f(1.25f, 1.2f, 0, 1);
        drawCube(stack, sunMatrix, sunColor);
        Matrix4f munMatrix = new Matrix4f().rotateXYZ(0.5f, 0.5f, 0.5f).setTranslation(munPos).scale(20);
        Vector4f munColor = new Vector4f(0.95f, 0.88f, 1.f, 1);
        drawCube(stack, munMatrix, munColor);
    }
}
