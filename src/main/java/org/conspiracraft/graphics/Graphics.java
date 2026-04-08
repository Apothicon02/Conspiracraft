package org.conspiracraft.graphics;

import org.conspiracraft.graphics.buffers.CmdBuffer;
import org.conspiracraft.graphics.buffers.ubos.GlobalUBO;
import org.conspiracraft.graphics.buffers.ubos.UniformBuffer;
import org.lwjgl.system.MemoryStack;

import static org.conspiracraft.Window.window;
import static org.lwjgl.sdl.SDLInit.SDL_Quit;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.vulkan.VK14.*;

public class Graphics {
    public static Descriptors descriptors;

    public Graphics() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Device.init(stack);
            Swapchain.init(stack);
            createUniformBuffers(stack);
            descriptors = new Descriptors(stack);
            CmdBuffer.init(stack);
            SyncObjects.init(stack);
        }
    }

    public static void recreateDescriptors(MemoryStack stack) {if (descriptors != null) {descriptors = new Descriptors(stack);}}

    public static GlobalUBO globalUBO = new GlobalUBO();
    public static UniformBuffer globalUBOBuf;
    public void createUniformBuffers(MemoryStack stack) {
        globalUBOBuf = new UniformBuffer(stack, globalUBO.size(), VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, globalUBO);
    }

    public void cleanup() {
        SDL_DestroyWindow(window);
        SDL_Quit();
    }
}
