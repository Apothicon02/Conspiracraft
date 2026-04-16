package org.conspiracraft.player;

import org.conspiracraft.Main;
import org.conspiracraft.Window;
import org.conspiracraft.graphics.Renderer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.conspiracraft.Main.*;
import static org.conspiracraft.Settings.mouseSensitivity;
import static org.lwjgl.sdl.SDLKeyboard.SDL_GetKeyboardState;
import static org.lwjgl.sdl.SDLMouse.*;
import static org.lwjgl.sdl.SDLScancode.*;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowPosition;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowSize;

public class InputHandler {
    public InputHandler() {}

    public ByteBuffer keys;
    public ByteBuffer prevKeys;
    public int mouse;
    public boolean leftButtonPressed = false;
    public boolean middleButtonPressed = false;
    public boolean rightButtonPressed = false;
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
            player.rotate((float) -Math.toRadians(displVec.x * (mouseSensitivity / 10)),
                    (float) -Math.toRadians(displVec.y * (mouseSensitivity / 10)));

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
            if (keyRelease(SDL_SCANCODE_F)) {
                System.out.print(String.format("%.2f", fps)+"fps / "+String.format("%.2f", ms)+"ms\n");
            }
            if (keyRelease(SDL_SCANCODE_T)) {
                Main.timeNs+=10000000000L;
            }
            if (keyRelease(SDL_SCANCODE_B)) {
                float r = 0.f;
                if (isKeyDown(SDL_SCANCODE_R)) {
                    r = (float) Math.toRadians(45);
                }
                if (isKeyDown(SDL_SCANCODE_F3)) {
                    Renderer.cubes.removeLast();
                } else {
                    Renderer.cubes.addLast(new Matrix4f().translate(new Vector3f(player.pos).floor().add(0, 2, 0)).rotateXYZ(r, r, 0));
                }
            }

            MemoryUtil.memCopy(MemoryUtil.memAddress(keys), MemoryUtil.memAddress(prevKeys), keys.capacity());
        }
    }
    public void setInputs() {
        keys = SDL_GetKeyboardState();
        mouse = SDL_GetMouseState(null, null);
        leftButtonPressed = (mouse & SDL_BUTTON_LEFT) > 0;
        rightButtonPressed = (mouse & SDL_BUTTON_RIGHT) > 0;
        middleButtonPressed = (mouse & SDL_BUTTON_MIDDLE) > 0;
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
