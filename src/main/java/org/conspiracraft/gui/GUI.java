package org.conspiracraft.gui;

import kotlin.Pair;
import org.conspiracraft.Main;
import org.conspiracraft.Settings;
import org.conspiracraft.audio.AudioController;
import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.graphics.buffers.Buffer;
import org.conspiracraft.graphics.textures.ImageHelper;
import org.conspiracraft.graphics.textures.Texture3D;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.gui.buttons.*;
import org.conspiracraft.gui.sliders.*;
import org.conspiracraft.items.DurableItem;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.Recipes;
import org.conspiracraft.items.types.ItemType;
import org.conspiracraft.items.types.ItemTypes;
import org.conspiracraft.player.HandManager;
import org.conspiracraft.utils.Utils;
import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.conspiracraft.Main.*;
import static org.conspiracraft.Settings.height;
import static org.conspiracraft.Settings.width;
import static org.conspiracraft.graphics.Renderer.pushUBO;
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

    public static Slider drawingSlider = null;
    public static float sliderX = 0.f;
    public static List<Slider> sliders = new ArrayList<>();
    public static Button drawingButton = null;
    public static List<Button> buttons = new ArrayList<>();
    public static boolean canInteract = true;

    public static float guiScale = 1;
    public static float guiScaleMul = 4f; //even-though it's a float, should always be set to a round number to prevent distortion of pixel-art
    public static float aspectRatio = 0f;

    public static int hotbarSizeX = 282;
    public static int hotbarSizeY = 22;
    public static float hotbarPosX = 0.f;
    public static float hotbarPosY = 0.f;
    public static int slotSize = 20;
    public static int slotSizeY = 22;
    public static int enlargedSlotSize = 22;
    public static float containerPosY = 0;
    public static void update() {
        aspectRatio = (float) width / height;
        updateScale(1);
        hotbarPosX = (0.5f - ((hotbarSizeX / 2f) / guiScale));
        hotbarPosY = 5.f / height;
        containerPosY = hotbarPosY + (((hotbarSizeY * 5) / guiScale) * aspectRatio);
        buttons.clear();
        sliders.clear();
    }
    public static void updateScale(int scaleDiv) {
        guiScaleMul = (int) (((Math.min(width, height) / 270f)/scaleDiv)*Settings.guiScale);
        guiScale = (int)(width / guiScaleMul);
    }
    public static Vector4f color = new Vector4f(1.f);
    public static Object objectOnPrev = null;

    public static List<Menu> menus = new ArrayList<>();//List.of(new BarrelMenu().setName("Barrel").setPos(0.5f, 0.65f)));
    public static Vector2i cursorPos = new Vector2i();
    public static void tick() {
        boolean shouldSetObjectOnPrev = true;
        cursorPos.set(cursorPxX(), cursorPxY());
        color.set(1);
        pushUBO.updateTex(Textures.gui); //use gui atlas
        pushUBO.updateLayer(5); //button
        for (Button button : buttons) {
            if (cursorPos.x() > button.bounds.x() && cursorPos.x() < button.bounds.z() && cursorPos.y() > button.bounds.y() && cursorPos.y() < button.bounds.w()) {
                Vector2i borderData = getButtonBorderData(button.width);
                pushUBO.updateAtlasOffset(new Vector2i(0, borderData.y()+128));
                drawQuad(false, false, (float) button.bounds.x() / width, (float) button.bounds.y() / height, button.width, 16);
                if (Main.player.inputHandler.leftButtonClick) {
                    button.clicked();
                } else if (objectOnPrev == null || !objectOnPrev.getClass().equals(button.getClass())) {
                    AudioController.playHoverSound();
                }
                objectOnPrev = button;
                shouldSetObjectOnPrev = false;
            }
        }
        for (Slider slider : sliders) {
            if (cursorPos.x() > slider.bounds.x() && cursorPos.x() < slider.bounds.z() && cursorPos.y() > slider.bounds.y() && cursorPos.y() < slider.bounds.w()) {
                pushUBO.updateAtlasOffset(new Vector2i(277, 320));
                drawQuad(true, false, (float) cursorPos.x() / width, (float) slider.bounds.y() / height, 5, 16);
                if (Main.player.inputHandler.leftButtonPressed) {
                    slider.pressed(cursorPos.x());
                } else if (objectOnPrev == null || !objectOnPrev.getClass().equals(slider.getClass())) {
                    AudioController.playHoverSound();
                }
                objectOnPrev = slider;
                shouldSetObjectOnPrev = false;
            }
        }
        if (shouldSetObjectOnPrev) {
            objectOnPrev = null;
        }
    }
    public static boolean showUI = true;
    public static void draw() {
        //long startTime = System.nanoTime();
        update();
        if (showUI) {
            for (Menu menu : menus) {if (menu != null) {menu.update(); menu.draw();}}
            if (!GUI.pauseMenuOpen) {
                pushUBO.updateTex(Textures.gui); //use gui atlas
                pushUBO.updateLayer(3); //frame
                pushUBO.updateSize(new Vector2i(1));
                pushUBO.updateAtlasOffset(new Vector2i(0));
                color.set(1.f);
                drawQuad(true, true, 0.5f, 0.5f, 1, 1); //crosshair
                drawInventory();
            }
            drawDebug();
        }
        drawAlwaysVisible();
        //System.out.println((System.nanoTime() - startTime) + "ns to draw gui");
        tick();
    }
    public static Vector4f menuBgColor = new Vector4f(1.f);
    public static void drawAlwaysVisible() {
        pushUBO.updateTex(Textures.gui); //use gui atlas
        if (Main.isSaving || pauseMenuOpen) {
            color.set(1.f);
            Vector2i border = new Vector2i((int) ((32 * (width / 3840f)) / guiScaleMul), (int) ((32 * (height / 2180f)) / guiScaleMul));
            if (Main.isSaving) {
                drawText(false, 0, 0, 2 + border.x(), 2 + border.y(), "Saving data...".toCharArray());
            }
            if (pauseMenuOpen) {
                color = menuBgColor;
                pushUBO.updateLayer(3); //frame
                pushUBO.updateSize(new Vector2i(3840, 2160));
                //glUniform2i(Renderer.gui.uniforms.get("scale"), width, (int) (height * aspectRatio));
                pushUBO.updateAtlasOffset(new Vector2i(0));
                Renderer.drawQuadCentered(new Matrix4f(), new Vector4f(1));
            }
        }

        color.set(1.f);
        pushUBO.updateLayer(0); //text
        if (showUI) {
            if (graphicsSettingMenuOpen) {
                menuBgColor = new Vector4f(1.f, 0.9f, 0.5f, 1.f);
                drawText(true, 0.5f, 1, 0, -10 - charHeight, "Graphics Settings".toCharArray());
                drawingButton = new BackButton();
                drawButton(true, 0.5f, 0.5f, 0, (charHeight * 5) + 2, "Back To Settings Menu".toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingSlider = new FoVSlider();
                sliderX = (Settings.fov - 30) / 150;
                drawSlider(true, 0.5f, 0.5f, 0, (charHeight * 3) + 1, ("Field of View:" + String.format("%.1f", (sliderX * 150) + 30)).toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingButton = new DynamicFoVButton();
                drawButton(true, 0.5f, 0.5f, -35.5f, charHeight, (Settings.dynamicFoVEnabled ? " FoV VFX " : "No FoV VFX").toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingButton = new UpscaleButton();
                drawButton(true, 0.5f, 0.5f, 35.5f, charHeight, (Settings.upscaled ? "Upscaled" : "  Native  ").toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingButton = new ShadowsButton();
                drawButton(true, 0.5f, 0.5f, -35.5f, (-charHeight) - 1, (Settings.shadowsEnabled ? "Shadowed" : "Unshadowed").toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingButton = new TAAButton();
                drawButton(true, 0.5f, 0.5f, 35.5f, (-charHeight) - 1, (Settings.taaEnabled ? "   TAA   " : "  No AA  ").toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingButton = new ReflectionsButton();
                drawButton(true, 0.5f, 0.5f, 0, (charHeight * -3) - 2, (Settings.reflectionsEnabled ? "Reflections Enabled" : "Reflections Disabled").toCharArray(), menuBgColor, new Vector4f(1.f));
            } else if (controlsSettingMenuOpen) {
                menuBgColor = new Vector4f(0.75f, 1.f, 0.75f, 1.f);
                drawText(true, 0.5f, 1, 0, -10 - charHeight, "Control Settings".toCharArray());
                drawingButton = new BackButton();
                drawButton(true, 0.5f, 0.5f, 0, (charHeight * 5) + 2, "Back To Settings Menu".toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingSlider = new SensitivitySlider();
                sliderX = Settings.mouseSensitivity;
                drawSlider(true, 0.5f, 0.5f, 0, (charHeight * 3) + 1, ("Sensitivity:" + String.format("%.1f", sliderX * 100) + "%").toCharArray(), menuBgColor, new Vector4f(1.f));
                drawButton(true, 0.5f, 0.5f, 0, charHeight, "Keybind Settings".toCharArray(), menuBgColor, new Vector4f(1.f));
            } else if (audioSettingMenuOpen) {
                menuBgColor = new Vector4f(0.9f, 0.75f, 1.f, 1.f);
                drawText(true, 0.5f, 1, 0, -10 - charHeight, "Audio Settings".toCharArray());
                drawingButton = new BackButton();
                drawButton(true, 0.5f, 0.5f, 0, (charHeight * 5) + 2, "Back To Settings Menu".toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingSlider = new VolumeSlider();
                sliderX = AudioController.masterVolume / 2.f;
                drawSlider(true, 0.5f, 0.5f, 0, (charHeight * 3) + 1, ("Master Volume:" + String.format("%.1f", sliderX * 200) + "%").toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingButton = new MuteButton();
                drawButton(true, 0.5f, 0.5f, -35.5f, charHeight, (AudioController.muted ? "  Muted  " : " Unmuted ").toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingButton = new AudioChannelButton();
                drawButton(true, 0.5f, 0.5f, 35.5f, charHeight, AudioController.getOutputModeAsTxt().toCharArray(), menuBgColor, new Vector4f(1.f));
            } else if (accessibilitySettingMenuOpen) {
                menuBgColor = new Vector4f(0.72f, 0.725f, 1.f, 1.f);
                drawText(true, 0.5f, 1, 0, -10 - charHeight, "Accessibility Settings".toCharArray());
                drawingButton = new BackButton();
                drawButton(true, 0.5f, 0.5f, 0, (charHeight * 5) + 2, "Back To Settings Menu".toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingButton = new GUIScaleButton();
                drawButton(true, 0.5f, 0.5f, 0, (charHeight * 3) + 1, ("GUI Scale:" + Settings.guiScale).toCharArray(), menuBgColor, new Vector4f(1.f));
            } else if (settingMenuOpen) {
                menuBgColor = new Vector4f(0.93f, 0.85f, 1.0f, 1.f);
                drawText(true, 0.5f, 1, 0, -10 - charHeight, "Settings".toCharArray());
                drawingButton = new BackButton();
                drawButton(true, 0.5f, 0.5f, 0, (charHeight * 5) + 2, "Back To Main Menu".toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingButton = new AudioSettingsButton();
                drawButton(true, 0.5f, 0.5f, -35.5f, (charHeight * 3) + 1, "  Audio  ".toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingButton = new ControlsSettingsButton();
                drawButton(true, 0.5f, 0.5f, 35.5f, (charHeight * 3) + 1, "Controls".toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingButton = new GraphicsSettingsButton();
                drawButton(true, 0.5f, 0.5f, 0, charHeight, "    Graphics    ".toCharArray(), menuBgColor, new Vector4f(1.f));
                drawingButton = new AccessibilitySettingsButton();
                drawButton(true, 0.5f, 0.5f, 0, (-charHeight) - 1, "Accessibility".toCharArray(), menuBgColor, new Vector4f(1.f));
            } else if (pauseMenuOpen) {
                drawText(true, 0.5f, 1, 0, -10 - charHeight, "Paused".toCharArray());
                drawingButton = new BackButton();
                drawButton(true, 0.5f, 0.5f, 0, (charHeight * 5) + 2, "Continue Playing".toCharArray(), new Vector4f(1.f), new Vector4f(1.f));
                char[] saveChars = "Save World".toCharArray();
                drawingButton = new SaveWorldButton();
                drawButton(true, 0.5f, 0.5f, -35.5f, (charHeight * 3) + 1, saveChars, new Vector4f(1.f), new Vector4f(1.f));
                drawingButton = new SettingsButton();
                drawButton(true, 0.5f, 0.5f, 35.5f, (charHeight * 3) + 1, "Settings".toCharArray(), new Vector4f(1.f), new Vector4f(1.f));
                drawingButton = new LanguageButton();
                drawButton(true, 0.5f, 0.5f, 0, charHeight, "                ".toCharArray(), new Vector4f(1.f), new Vector4f(1.f));
                drawText(true, 0.5f, 0.5f, 0, charHeight, Languages.translate("name").toCharArray());
                drawingButton = new QuitToMenuButton();
                drawButton(true, 0.5f, 0.5f, 0, (-charHeight) - 1, "Quit To Menu".toCharArray(), new Vector4f(1.f), new Vector4f(1.f));
                drawingButton = new QuitToDesktopButton();
                drawButton(true, 0.5f, 0.5f, 0, (charHeight * -3) - 2, "Quit To Desktop".toCharArray(), new Vector4f(1.f), new Vector4f(1.f));
            }
        }
        menuBgColor = new Vector4f(1.f);
    }
    public static boolean showDebug = true;
    public static void drawDebug() {
        color.set(1.f, 1.f, 1.f, 0.5f);
        pushUBO.updateLayer(0); //text
        pushUBO.updateTex(Textures.gui); //use gui atlas
        int pauseOff = (pauseMenuOpen ? -6 : -2);
        drawText(false, 0, 1, -pauseOff-1, pauseOff - (charHeight*0.5f), (String.format("%.2f", fps) + "fps ").toCharArray(), 2);
        drawText(false, 0, 1, -0.5f-pauseOff, 1+pauseOff - charHeight, (String.format("%.2f", ms) + "ms").toCharArray(), 3);
        if (showDebug && !pauseMenuOpen) {
            drawText(false, 0, 1, 1.5f, -1.34f - (charHeight*1.33f), ((int) Main.player.pos.x + "x," + (int) Main.player.pos.y + "y," + (int) Main.player.pos.z + "z").toCharArray(), 3);
        }
    }
    public static void drawInventory() {
        color.set(1.f);
        pushUBO.updateTex(Textures.items); //use item atlas
        if (Main.player.inv.cursorItem != null) { //cursor item
            ItemType itemType = Main.player.inv.cursorItem.type;
            pushUBO.updateAtlasOffset(itemType.atlasOffset);
            float offX = Main.player.inputHandler.currentPos.x() / width;
            float offY = Math.abs(height - (Main.player.inputHandler.currentPos.y())) / height;
            drawQuad(true, true, offX, offY, ItemTypes.itemTexSize, ItemTypes.itemTexSize);
            if (Main.player.inv.cursorItem.amount > 1) {
                pushUBO.updateTex(Textures.gui); //use gui atlas
                char[] chars = Main.player.inv.cursorItem.amountString().toCharArray();
                float startOffset = 16 - (chars.length * (charWidth/2.f));
                drawText(false, offX, offY, 1 + startOffset - (charWidth * 1.5f), 1 - charHeight, chars, 2);
            }
        }
    }
    public static void drawSlider(boolean centered, float offsetX, float offsetY, float offsetPX, float offsetPY, char[] chars, Vector4f bgColor, Vector4f txtColor) {
        pushUBO.updateLayer(5); //button/slider
        Vector2i borderData = getButtonBorderData((charWidth * chars.length) + 6);
        pushUBO.updateAtlasOffset(new Vector2i(0, borderData.y()+224));
        color = bgColor;
        drawSlot(true, false, offsetX, offsetY, offsetPX - 1, offsetPY - 4, 0, 0, borderData.x() + 2, 16);
        drawingSlider = null;
        pushUBO.updateAtlasOffset(new Vector2i(272, 320));
        color.set(bgColor.x(), bgColor.y(), bgColor.z(), 1);
        float posX = (sliderX-0.5f)*borderData.x();
        drawSlot(true, false, offsetX, offsetY, (offsetPX+posX) - 1, offsetPY - 4, 0, 0, 5, 16);
        sliderX = 0.f;
        pushUBO.updateLayer(0); //text
        color = txtColor;
        float size = chars.length * charWidth;
        float centeredOffset = centered ? size / 2 : 0.f;
        float offset = 0;
        for (char character : chars) {
            int charAtlasOffset = getCharAtlasOffset(character);
            if (charAtlasOffset >= 0) {
                pushUBO.updateAtlasOffset(new Vector2i(charAtlasOffset, 0));
                drawSlot(offsetX, offsetY, offsetPX + offset - centeredOffset, offsetPY, 0, 0, charWidth, charHeight);
            }
            offset += charWidth;
        }
    }
    public static void drawButton(boolean centered, float offsetX, float offsetY, float offsetPX, float offsetPY, char[] chars, Vector4f bgColor, Vector4f txtColor) {
        pushUBO.updateLayer(5); //button
        Vector2i borderData = getButtonBorderData((charWidth * chars.length) + 6);
        pushUBO.updateAtlasOffset(new Vector2i(0, borderData.y()));
        color = bgColor;
        drawSlot(centered, false, offsetX, offsetY, offsetPX - 1, offsetPY - 4, 0, 0, borderData.x() + 2, 16);
        drawingButton = null;

        pushUBO.updateLayer(0); //text
        color = txtColor;
        drawText(centered, offsetX, offsetY, offsetPX, offsetPY, chars);
    }
    public static void drawText(boolean centered, float offsetX, float offsetY, float offsetPX, float offsetPY, char[] chars) {
        drawText(centered, offsetX, offsetY, offsetPX, offsetPY, chars, 1);
    }
    public static void drawText(boolean centered, float offsetX, float offsetY, float offsetPX, float offsetPY, char[] chars, int iScale) {
        int scaledCharWidth = charWidth/iScale;
        float size = chars.length * scaledCharWidth;
        float centeredOffset = centered ? size*0.5f : 0.f;
        float offset = 0;
        for (char character : chars) {
            int charAtlasOffset = getCharAtlasOffset(character);
            if (charAtlasOffset >= 0) {
                pushUBO.updateAtlasOffset(new Vector2i(charAtlasOffset, 0));
                drawSlot(offsetX, offsetY, (offsetPX + offset - centeredOffset) + (centered ? 0 : scaledCharWidth*0.5f), offsetPY, 0, 0, charWidth, charHeight, iScale);
            }
            offset += scaledCharWidth;
        }
    }
    public static Vector2i getButtonBorderData(int buttonWidth) {
        if (buttonWidth > 211) {
            return new Vector2i(282, 0);
        } else if (buttonWidth > 140) {
            return new Vector2i(211, 16);
        } else if (buttonWidth > 69) {
            return new Vector2i(140, 32);
        } else if (buttonWidth > 47) {
            return new Vector2i(69, 48);
        } else if (buttonWidth > 31) {
            return new Vector2i(47, 64);
        } else if (buttonWidth > 15) {
            return new Vector2i(31, 80);
        } else {
            return new Vector2i(16, 96);
        }
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
        drawSlot(false, false, offsetX, offsetY, offPxX, offPxY, x, y, sizeX, sizeY, 1);
    }
    public static void drawSlot(float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY, int iScale) {
        drawSlot(false, false, offsetX, offsetY, offPxX, offPxY, x, y, sizeX, sizeY, iScale);
    }
    public static void drawSlot(boolean centeredX, boolean centeredY, float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY) {
        drawSlot(centeredX, centeredY, offsetX, offsetY, offPxX, offPxY, x, y, sizeX, sizeY, 1);
    }
    public static void drawSlot(boolean centeredX, boolean centeredY, float offsetX, float offsetY, float offPxX, float offPxY, int x, int y, int sizeX, int sizeY, int iScale) {
        float selectedPosX = x * (slotSize / guiScale);
        float selectedPosY = y * ((slotSizeY / guiScale) * aspectRatio);
        drawQuad(centeredX, centeredY, selectedPosX + offsetX + (offPxX / guiScale), selectedPosY + (offsetY - (3.f / height)) + ((offPxY / guiScale) * aspectRatio), sizeX, sizeY, iScale);
    }
    public static void drawQuad(boolean centeredX, boolean centeredY, float x, float y, int scaleX, int scaleY) {drawQuad(centeredX, centeredY, x, y, scaleX, scaleY, 1);}
    public static void drawQuad(boolean centeredX, boolean centeredY, float x, float y, int scaleX, int scaleY, int iScale) {
        if (iScale > 1) {
            updateScale(iScale);
        }
        float xScale = (scaleX / guiScale);
        float yScale = (scaleY / guiScale) * aspectRatio;
        float xOffset = ((x * 2) - 1) + (centeredX ? 0 : xScale);
        float yOffset = ((y * -2) + 1) - (centeredY ? 0 : yScale);
        Vector2i offset = new Vector2i((int) ((x - (centeredX ? xScale / 2 : 0)) * width), (int) ((y + (centeredY ? yScale / 2 : 0)) * height));
        pushUBO.updateSize(new Vector2i(scaleX, scaleY));
        //glUniform2i(Renderer.gui.uniforms.get("offset"), offset.x(), offset.y());
        Vector2i scale = new Vector2i((int) (xScale * width), (int) (yScale * height));
        //glUniform2i(Renderer.gui.uniforms.get("scale"), scale.x(), scale.y());
        if (drawingButton != null) {
            drawingButton.bounds = new Vector4i(offset.x(), offset.y(), offset.x() + scale.x(), offset.y() + scale.y());
            drawingButton.width = scaleX;
            buttons.add(drawingButton);
        } else if (drawingSlider != null) {
            drawingSlider.bounds = new Vector4i(offset.x(), offset.y(), offset.x() + scale.x(), offset.y() + scale.y());
            drawingSlider.width = scaleX;
            sliders.add(drawingSlider);
        }
        Renderer.drawQuadCentered(new Matrix4f().translate(xOffset, yOffset, 0.f).scale(xScale, yScale, 1), color);
        if (iScale > 1) {
            updateScale(1);
        }
    }

    public static int charWidth = 6;
    public static int charHeight = 8;
    public static char[] alphabet = """
            0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz.!?$:,;`'"()[]{}*=+-/\\^%&#~<>|_\s""".toCharArray();
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
            guiTexDepth = 0;
            loadImage(stagingBuffer, "texture/font");
            loadImage(stagingBuffer, "texture/hotbar");
            loadImage(stagingBuffer, "texture/selected_slot");
            loadImage(stagingBuffer, "texture/frame");
            loadImage(stagingBuffer, "texture/trash");
            loadImage(stagingBuffer, "texture/button");

            ImageHelper.fillImage(stack, Textures.gui, stagingBuffer);

            ItemTypes.fillTexture(stack);
        }
    }

    public static long guiTexDepth = 0;
    public static int layerSize = 4*Textures.gui.width*Textures.gui.height;
    public static ByteBuffer layerBuffer = ByteBuffer.allocateDirect(layerSize);
    public static void loadImage(Buffer stagingBuffer, String path) throws IOException {
        layerBuffer.clear();
        Utils.imageToBuffer(layerBuffer, Textures.gui.width, Textures.gui.height, Utils.loadImage("gui/" + path));
        memCopy(memAddress(layerBuffer), stagingBuffer.pointer.get(0)+(guiTexDepth*layerSize), layerSize);
        guiTexDepth++;
    }
}
