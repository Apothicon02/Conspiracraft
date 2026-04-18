package org.conspiracraft;

import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.graphics.buffers.Buffer;
import org.conspiracraft.graphics.textures.ImageHelper;
import org.conspiracraft.graphics.textures.Texture3D;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.utils.Utils;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.conspiracraft.Settings.height;
import static org.conspiracraft.Settings.width;
import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.vulkan.VK10.*;

public class GUI {
    public static float guiScale = 1;
    public static float guiScaleMul = 4f; //even-though it's a float, should always be set to a round number to prevent distortion of pixel-art
    public static float aspectRatio = 0f;

    public static int hotbarSizeX = 282;
    public static int hotbarSizeY = 22;
    public static float hotbarPosX = 0.f;
    public static float hotbarPosY = 0.f;
    public static int slotSize = 20;
    public static int slotSizeY = 22;
    public static int enlargedSlotSize = 24;
    public static void update() {
        guiScaleMul = (int) (Math.min(width, height) / 270f);
        guiScale = width / guiScaleMul;
        aspectRatio = (float) width / height;
        hotbarPosX = (0.5f - ((hotbarSizeX / 2f) / guiScale));
        hotbarPosY = 5.f / height;
    }
    public static void draw() {
        update();
        //Renderer.drawQuad(new Matrix4f().translate(-1.f, -1.f, 0.f).scale(1), new Vector4f(0.5f, 1.f, 0.5f, 1.f));
        pushUBO.updateLayer(1); //inventory
        drawQuad(false, false, hotbarPosX, hotbarPosY, hotbarSizeX, hotbarSizeY); //hotbar
    }

    public static void drawQuad(boolean centeredX, boolean centeredY, float x, float y, int scaleX, int scaleY) {
        float xScale = (scaleX / guiScale);
        float yScale = (scaleY / guiScale) * aspectRatio;
        float xOffset = ((x * 2) - 1) + (centeredX ? 0 : xScale);
        float yOffset = ((y * -2) + 1) - (centeredY ? 0 : yScale);
        //Vector2i offset = new Vector2i((int) ((x - (centeredX ? xScale / 2 : 0)) * width), (int) ((y + (centeredY ? yScale / 2 : 0)) * height));
        pushUBO.updateSize(new Vector2i(scaleX, scaleY));
//        glUniform2i(Renderer.gui.uniforms.get("offset"), offset.x(), offset.y());
//        glUniform2i(Renderer.gui.uniforms.get("size"), scaleX, scaleY);
//        Vector2i scale = new Vector2i((int) (xScale * width), (int) (yScale * height));
//        glUniform2i(Renderer.gui.uniforms.get("scale"), scale.x(), scale.y());
//        if (drawingButton != null) {
//            drawingButton.bounds = new Vector4i(offset.x(), offset.y(), offset.x() + scale.x(), offset.y() + scale.y());
//            drawingButton.width = scaleX;
//            buttons.add(drawingButton);
//        } else if (drawingSlider != null) {
//            drawingSlider.bounds = new Vector4i(offset.x(), offset.y(), offset.x() + scale.x(), offset.y() + scale.y());
//            drawingSlider.width = scaleX;
//            sliders.add(drawingSlider);
//        }
        Renderer.drawQuadCentered(new Matrix4f().translate(xOffset, yOffset, 0.f).scale(xScale, yScale, 1), new Vector4f(1.f));
    }

    public static int charWidth = 6;
    public static int charHeight = 8;
    public static char[] alphabet = """
            0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz.!?$:,;`'"()[]{}*=+-/\\^%&#~<>|
            """.toCharArray();
    public static Map<Character, Integer> charAtlasOffsetIndex = new HashMap<>();
    public static char space = " ".toCharArray()[0];

    public static int getCharAtlasOffset(char character) {
        return character == space ? -1 : charAtlasOffsetIndex.get(character);
    }

    public static void fillTexture() throws IOException {
        int i = 0;
        for (char character : alphabet) {
            charAtlasOffsetIndex.put(character, i);
            i += charWidth;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Buffer stagingBuffer = new Buffer(stack, Textures.gui.width*Textures.gui.height*((Texture3D)Textures.gui).depth*4, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, true);

            loadImage(stagingBuffer, "texture/font");
            loadImage(stagingBuffer, "texture/hotbar");
            loadImage(stagingBuffer, "texture/selected_slot");
            loadImage(stagingBuffer, "texture/frame");
            loadImage(stagingBuffer, "texture/trash");
            loadImage(stagingBuffer, "texture/button");

            //ItemTypes.fillTexture();
            ImageHelper.fillImage(stack, Textures.gui, stagingBuffer);
        }
    }

    public static int guiTexDepth = 0;
    public static int layerSize = 4*Textures.gui.width*Textures.gui.height;
    public static ByteBuffer layerBuffer = ByteBuffer.allocateDirect(layerSize);
    public static void loadImage(Buffer stagingBuffer, String path) throws IOException {
        layerBuffer.clear();
        Utils.imageToBuffer(layerBuffer, Textures.gui.width, Textures.gui.height, ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/gui/" + path + ".png")));
        memCopy(memAddress(layerBuffer), stagingBuffer.pointer.get(0)+(guiTexDepth*layerSize), layerSize);
        guiTexDepth++;
    }
}
