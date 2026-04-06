package org.conspiracraft.renderer;

import org.conspiracraft.Constants;
import org.conspiracraft.Main;
import org.conspiracraft.Settings;
import org.conspiracraft.player.InputHandler;
import org.joml.Matrix4f;

import static org.conspiracraft.Main.events;
import static org.conspiracraft.Settings.*;
import static org.lwjgl.sdl.SDLError.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLLog.*;
import static org.lwjgl.sdl.SDLMouse.SDL_SetWindowRelativeMouseMode;
import static org.lwjgl.sdl.SDLVideo.*;

public class Window {
    public static long window;
    public static long context;
    public boolean tenBitColorMode = true;

    public Window() {
        if (!SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO)) {throw new IllegalStateException("Unable to initialize SDL");}
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 3);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        if (tenBitColorMode) {
            SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 10);
            SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 10);
            SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 10);
            SDL_GL_SetAttribute(SDL_GL_ALPHA_SIZE, 2);
        }
        SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, 24);
        SDL_GL_SetAttribute(SDL_GL_FRAMEBUFFER_SRGB_CAPABLE, 0);
        SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);

        window = SDL_CreateWindow(Constants.GAME_NAME, width, height, SDL_WINDOW_OPENGL | SDL_WINDOW_RESIZABLE | SDL_WINDOW_HIGH_PIXEL_DENSITY);
        if (window == 0) {SDL_LogCritical(SDL_LOG_CATEGORY_APPLICATION, "Failed to create window: %s\n"+SDL_GetError());SDL_Quit();}
        context = SDL_GL_CreateContext(window);
        SDL_SetWindowResizable(window, true);
        SDL_SetWindowRelativeMouseMode(window, true);
        SDL_GL_SetSwapInterval(0); //disable vsync
        SDL_GL_MakeCurrent(Window.window, Window.context);
        SDL_PumpEvents();
    }

    public void update() {
        SDL_GL_SwapWindow(window);
    }

    public void resized(int width, int height) {
        Settings.width = width;
        Settings.height = height;
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