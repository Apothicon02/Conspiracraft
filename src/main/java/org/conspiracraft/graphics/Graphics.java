package org.conspiracraft.graphics;

import org.lwjgl.system.MemoryStack;

import static org.conspiracraft.Window.window;
import static org.lwjgl.sdl.SDLInit.SDL_Quit;
import static org.lwjgl.sdl.SDLVideo.*;

public class Graphics {
    public Graphics() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Device.init(stack);
            Swapchain.init(stack);
            Pipeline.init(stack);
            CmdBuffer.init(stack);
            SyncObjects.init(stack);
        }
    }

    public void cleanup() {
        SDL_DestroyWindow(window);
        SDL_Quit();
    }
}
