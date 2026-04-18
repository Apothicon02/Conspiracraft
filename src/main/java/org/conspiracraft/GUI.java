package org.conspiracraft;

import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.graphics.buffers.Buffer;
import org.conspiracraft.graphics.textures.ImageHelper;
import org.conspiracraft.graphics.textures.Texture3D;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.player.HandManager;
import org.conspiracraft.utils.Utils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
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
import static org.conspiracraft.player.Inventory.invWidth;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.vulkan.VK10.*;

public class GUI {
    public static boolean audioSettingMenuOpen = false;
    public static boolean controlsSettingMenuOpen = false;
    public static boolean graphicsSettingMenuOpen = false;
    public static boolean accessibilitySettingMenuOpen = false;
    public static boolean settingMenuOpen = false;
    public static boolean pauseMenuOpen = false;
    public static boolean inventoryOpen = false;

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
    public static float containerPosY = 0;
    public static void update() {
        guiScaleMul = (int) (Math.min(width, height) / 270f);
        guiScale = width / guiScaleMul;
        aspectRatio = (float) width / height;
        hotbarPosX = (0.5f - ((hotbarSizeX / 2f) / guiScale));
        hotbarPosY = 5.f / height;
        containerPosY = hotbarPosY + (((hotbarSizeY * 5) / guiScale) * aspectRatio);
    }
    public static void draw() {
        update();
        //Renderer.drawQuad(new Matrix4f().translate(-1.f, -1.f, 0.f).scale(1), new Vector4f(0.5f, 1.f, 0.5f, 1.f));
        pushUBO.updateLayer(1); //inventory
        drawQuad(false, false, hotbarPosX, hotbarPosY, hotbarSizeX, hotbarSizeY); //hotbar
        pushUBO.updateLayer(2); //selector
        Main.player.inv.selectedSlot = new Vector2i(HandManager.hotbarSlot, 0);
        Vector2i selSlot = null;
        if (GUI.inventoryOpen) {
            Vector2f clampedPos = confineToMenu(hotbarPosX, hotbarPosY, hotbarSizeX, hotbarSizeY * 4);
            if (clampedPos.x() > -1 && clampedPos.y() > -1) {
                Main.player.inv.selectedSlot = new Vector2i((int) (clampedPos.x() * invWidth), (int) (clampedPos.y() * 4));
                selSlot = Main.player.inv.selectedSlot;
            } else if (Main.player.creative) {
                Main.player.inv.selectedSlot = null;
                clampedPos = confineToMenu(hotbarPosX, containerPosY, hotbarSizeX, hotbarSizeY * 4);
                Main.player.inv.selectedContainerSlot = new Vector2i((int) (clampedPos.x() * invWidth), (int) (clampedPos.y() * 4));
                selSlot = Main.player.inv.selectedContainerSlot;
            }
        } else {
            Main.player.inv.selectedSlot = new Vector2i(HandManager.hotbarSlot, 0);
            selSlot = Main.player.inv.selectedSlot;
        }
        drawSlot(hotbarPosX, selSlot == Main.player.inv.selectedContainerSlot ? containerPosY : hotbarPosY, 0, -1, selSlot.x(), selSlot.y(), enlargedSlotSize, enlargedSlotSize); //selector
    }

    public static float cursorX() {
        return Main.player.inputHandler.currentPos.x() / width;
    }
    public static int cursorPxX() {
        return (int) Main.player.inputHandler.currentPos.x();
    }
    public static float cursorY() {
        return Math.abs(height - Main.player.inputHandler.currentPos.y()) / height;
    }
    public static int cursorPxY() {
        return (int) Math.abs(height - Main.player.inputHandler.currentPos.y());
    }
    public static float cut(float in, float min, float max) {
        if (in < min || in > max) {
            return -1;
        }
        return in;
    }
    public static float relative(float cursor, float pos, float size) {
        return (cut(cursor, pos, pos + size - (1f / width)) - pos) * (1 / size);
    }
    public static Vector2f confineToMenu(float posX, float posY, int sizeX, int sizeY) {
        return new Vector2f(
                relative(cursorX(), posX, sizeX / guiScale),
                relative(cursorY(), posY, (sizeY / guiScale) * aspectRatio)
        );
    }
    public static void drawSlot(float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY) {
        drawSlot(false, false, offsetX, offsetY, offPxX, offPxY, x, y, sizeX, sizeY);
    }
    public static void drawSlot(boolean centeredX, boolean centeredY, float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY) {
        float selectedPosX = x * (slotSize / guiScale);
        float selectedPosY = y * ((slotSizeY / guiScale) * aspectRatio);
        drawQuad(centeredX, centeredY, selectedPosX + offsetX + (offPxX / guiScale), selectedPosY + (offsetY - (3.f / height)) + ((offPxY / guiScale) * aspectRatio), sizeX, sizeY);
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
