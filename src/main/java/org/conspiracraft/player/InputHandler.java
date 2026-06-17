package org.conspiracraft.player;

import org.conspiracraft.entities.AnimalEntity;
import org.conspiracraft.entities.EntityTypes;
import org.conspiracraft.gui.GUI;
import org.conspiracraft.Main;
import org.conspiracraft.Window;
import org.conspiracraft.audio.AudioController;
import org.conspiracraft.graphics.Renderer;
import org.conspiracraft.items.Item;
import org.conspiracraft.items.types.ItemTypes;
import org.conspiracraft.world.World;
import org.joml.*;
import org.lwjgl.openal.AL10;
import org.lwjgl.system.MemoryUtil;

import java.lang.Math;
import java.nio.ByteBuffer;

import static org.conspiracraft.Main.*;
import static org.conspiracraft.Settings.mouseSensitivity;
import static org.conspiracraft.world.World.*;
import static org.lwjgl.sdl.SDLKeyboard.SDL_GetKeyboardState;
import static org.lwjgl.sdl.SDLKeyboard.SDL_GetModState;
import static org.lwjgl.sdl.SDLMouse.*;
import static org.lwjgl.sdl.SDLScancode.*;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowPosition;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowSize;

public class InputHandler {
    public InputHandler() {}

    public int keyMods;
    public ByteBuffer keys;
    public ByteBuffer prevKeys;
    public int mouse;
    public boolean leftButtonPressed = false;
    public boolean middleButtonPressed = false;
    public boolean rightButtonPressed = false;
    public boolean leftButtonClick = false;
    public boolean middleButtonClick = false;
    public boolean rightButtonClick = false;
    public Vector2f scroll = new Vector2f(0);
    public Vector2f displVec = new Vector2f(0);
    public Vector2f currentPos = new Vector2f(0);
    public void init() {
        keys = SDL_GetKeyboardState();
        prevKeys = MemoryUtil.memAlloc(keys.capacity());
        resetInputs();
    }
    public boolean isFullscreen = false;
    public void update() {
        if (Window.focused && Renderer.initialized) {
            if (keyRelease(SDL_SCANCODE_ESCAPE)) {
                if (GUI.accessibilitySettingMenuOpen) {
                    GUI.accessibilitySettingMenuOpen = false;
                } else if (GUI.graphicsSettingMenuOpen) {
                    GUI.graphicsSettingMenuOpen = false;
                } else if (GUI.controlsSettingMenuOpen) {
                    GUI.controlsSettingMenuOpen = false;
                } else if (GUI.audioSettingMenuOpen) {
                    GUI.audioSettingMenuOpen = false;
                } else if (GUI.settingMenuOpen) {
                    GUI.settingMenuOpen = false;
                } else if (GUI.inventoryOpen) {
                    GUI.inventoryOpen = false;
                } else {
                    GUI.pauseMenuOpen = !GUI.pauseMenuOpen;
                    if (GUI.pauseMenuOpen) {
                        AL10.alListenerf(AL10.AL_GAIN, AudioController.masterVolume);
                        timeMul = 0.f;
                    } else {
                        timeMul = 1.f;
                    }
                }
            } else {
                keyMods = SDL_GetModState();
                if (GUI.pauseMenuOpen || GUI.inventoryOpen) {
                    SDL_SetWindowRelativeMouseMode(Window.window, false);
                    //player.clearVars();
                    if (GUI.inventoryOpen) {
                        player.inv.tick();
                    }
                } else {
                    SDL_SetWindowRelativeMouseMode(Window.window, true);
                    player.rotate(displVec.x * (mouseSensitivity / 10), displVec.y * (mouseSensitivity / 10));
                    HandManager.input();
                }

                if (isKeyDown(SDL_SCANCODE_F3)) {
                    float r = 0.f;
                    if (isKeyDown(SDL_SCANCODE_R)) {
                        r = (float) Math.toRadians(45);
                    }
                    if (keyRelease(SDL_SCANCODE_X)) {
                        entitiesAddQueue.removeLast();
                    } else if (keyRelease(SDL_SCANCODE_V)) {
                        AnimalEntity entity = new AnimalEntity(Math.random() > 0.5f ? EntityTypes.SHEEP : EntityTypes.COW,
                                new Matrix4f().translate(new Vector3f(player.pos).add(0, player.eyeHeight, 0)).rotateXYZ(r, r, 0),
                                (float)(Math.random()*-0.1f));
                        entity.vel = player.camera.getForward().mul(2).add(player.vel);
                        entitiesAddQueue.addLast(entity);
                    }
                    if (keyRelease(SDL_SCANCODE_T)) {
                        Renderer.reloadAtlas = true;
                    }
                    if (keyRelease(SDL_SCANCODE_C)) {
                        player.creative = !player.creative;
                    }
                } else {
                    if (keyRelease(SDL_SCANCODE_F11)) {
                        if (!isFullscreen) {
                            isFullscreen = true;
                            SDL_SetWindowPosition(Window.window, 0, 0);
                            SDL_SetWindowSize(Window.window, 2560, 1440);
                            window.resized(2560, 1440);
                        } else {
                            isFullscreen = false;
                            SDL_SetWindowPosition(Window.window, 0, 32);
                            SDL_SetWindowSize(Window.window, (int) (2560 * 0.8f), (int) (1440 * 0.8f));
                            window.resized((int) (2560 * 0.8f), (int) (1440 * 0.8f));
                        }
                    }

                    if (keyRelease(SDL_SCANCODE_F1)) {
                        GUI.showUI = !GUI.showUI;
                    }

                    if (!GUI.pauseMenuOpen) {
                        if (keyRelease(SDL_SCANCODE_TAB)) {
                            GUI.inventoryOpen = !GUI.inventoryOpen;
                        }
                        if (keyRelease(SDL_SCANCODE_Q)) {
                            Item item = player.inv.getSelectedItem(false);
                            if (item != null) {
                                World.dropItem(item);
                                item.amount(0).type(ItemTypes.AIR);
                            }
                        }
                        if (keyRelease(SDL_SCANCODE_T)) {
                            timeMul = (timeMul == 1 ? (isKeyDown(SDL_SCANCODE_LSHIFT) ? 1000 : 10) : 1);
                        }
                        if (keyRelease(SDL_SCANCODE_B)) {
                            Vector3i startPos = new Vector3i((int)player.pos.x(), (int)player.pos.y(), (int)player.pos.z()).div(chunkSize).mul(chunkSize);
                            for (int z = startPos.z(); z < startPos.z()+chunkSize; z++) {
                                for (int x = startPos.x(); x < startPos.x() + chunkSize; x++) {
                                    for (int y = startPos.y(); y < startPos.y() + chunkSize; y++) {
                                        if ((z == startPos.z() || z == startPos.z()+chunkSize-1) || (x == startPos.x() || x == startPos.x()+chunkSize-1) || (y == startPos.y() || y == startPos.y()+chunkSize-1)) {
                                            World.setBlock(x, y, z, 1, 15);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Main.player.movementInputs();

            MemoryUtil.memCopy(MemoryUtil.memAddress(keys), MemoryUtil.memAddress(prevKeys), keys.capacity());
        }
    }
    public void setInputs() {
        keys = SDL_GetKeyboardState();
        mouse = SDL_GetMouseState(null, null);
        boolean leftButtonNowPressed = (mouse & SDL_BUTTON_LMASK) > 0;
        boolean rightButtonNowPressed = (mouse & SDL_BUTTON_RMASK) > 0;
        boolean middleButtonNowPressed = (mouse & SDL_BUTTON_MMASK) > 0;
        leftButtonClick = (!leftButtonNowPressed && leftButtonPressed);
        rightButtonClick = (!rightButtonNowPressed && rightButtonPressed);
        middleButtonClick = (!middleButtonNowPressed && middleButtonPressed);
        leftButtonPressed = leftButtonNowPressed;
        rightButtonPressed = rightButtonNowPressed;
        middleButtonPressed = middleButtonNowPressed;
    }
    public void resetInputs() {
        displVec.set(0);
        MemoryUtil.memSet(MemoryUtil.memAddress(keys), 0, keys.capacity());
        MemoryUtil.memSet(MemoryUtil.memAddress(prevKeys), 0, prevKeys.capacity());
        mouse = 0;
        leftButtonPressed = false;
        rightButtonPressed = false;
        middleButtonPressed = false;
    }
    public boolean keyRelease(int scanCode) {return (!isKeyDown(scanCode)) && wasKeyDown(scanCode);}
    public boolean isKeyDown(int scanCode) {return keys.get(scanCode) > 0;}
    public boolean wasKeyDown(int scanCode) {return prevKeys.get(scanCode) > 0;}
}
