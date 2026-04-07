package org.conspiracraft;

import org.conspiracraft.graphics.Graphics;
import org.conspiracraft.player.InputHandler;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import static org.conspiracraft.Main.events;
import static org.conspiracraft.Settings.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLVideo.*;

public class Window {
    public static long window;
    public static Graphics graphics;

    public Window() {
        if (!SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO)) {throw new IllegalStateException("Unable to initialize SDL");}

        try (MemoryStack stack = MemoryStack.stackPush()) {
            graphics = new Graphics();
        }
    }

    public void resized(int width, int height) {
        Settings.width = width;
        Settings.height = height;
        //graphics.recreateSwapchain();
    }

    public static boolean focused = false;
    public void pollEvents() {
        InputHandler inputHandler = Main.player.inputHandler;
        inputHandler.displVec.x = 0;
        inputHandler.displVec.y = 0;
        inputHandler.scroll.set(0);
        while (SDL_PollEvent(events)) {
            switch (events.type()) {
                case SDL_EVENT_QUIT:
                    Main.isClosing = true;
                    break;
                case SDL_EVENT_WINDOW_RESIZED:
                    resized(events.window().data1(), events.window().data2());
                    break;
                case SDL_EVENT_MOUSE_MOTION:
                    inputHandler.displVec.y += events.motion().xrel();
                    inputHandler.displVec.x += events.motion().yrel();
                    inputHandler.currentPos.x = events.motion().x();
                    inputHandler.currentPos.y = events.motion().y();
                    break;
                case SDL_EVENT_MOUSE_WHEEL:
                    inputHandler.scroll.x = events.wheel().x();
                    inputHandler.scroll.y = events.wheel().y();
                    break;
                case SDL_EVENT_WINDOW_DISPLAY_CHANGED:
                    //graphics.recreateSwapchain();
                    break;
                default:
                    break;
            }
        }
        long flags = SDL_GetWindowFlags(Window.window);
        focused = (flags & SDL_WINDOW_INPUT_FOCUS) != 0;
        if (focused) {inputHandler.setInputs();} else {inputHandler.resetInputs();}
    }

    private final Matrix4f projectionMatrix = new Matrix4f();
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
    public Matrix4f updateProjectionMatrix() {
        float aspectRatio = (float) width /height;
        projectionMatrix.identity();
        float FoV = (float)Math.toRadians(Main.player.camera.FOV);
        projectionMatrix.set(
                1.f/FoV, 0.f, 0.f, 0.f,
                0.f, -(aspectRatio/FoV), 0.f, 0.f,
                0.f, 0.f, 0.f, -1.f,
                0.f, 0.f, Constants.Z_NEAR, 0.f
        );
        return projectionMatrix;
    }
}