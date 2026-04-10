package org.conspiracraft.graphics;

import org.conspiracraft.graphics.buffers.CmdBuffer;
import org.conspiracraft.graphics.buffers.ShaderStorageBuffer;
import org.conspiracraft.graphics.buffers.ubos.GlobalUBO;
import org.conspiracraft.graphics.buffers.ubos.UniformBuffer;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.world.World;
import org.lwjgl.system.MemoryStack;

import static org.conspiracraft.Main.events;
import static org.conspiracraft.Window.window;
import static org.conspiracraft.graphics.Device.*;
import static org.conspiracraft.graphics.Pipeline.*;
import static org.conspiracraft.graphics.Swapchain.*;
import static org.conspiracraft.graphics.SyncObjects.*;
import static org.lwjgl.sdl.SDLEvents.SDL_PollEvent;
import static org.lwjgl.sdl.SDLInit.SDL_Quit;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.VK14.*;

public class Graphics {
    public static Descriptors descriptors;

    public Graphics() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Device.init(stack);
            Swapchain.init(stack);
            createBuffers(stack);
            Textures.generate(stack);
            descriptors = new Descriptors(stack);
            CmdBuffer.init(stack);
            SyncObjects.init(stack);
        }
    }

    public static GlobalUBO globalUBO = new GlobalUBO();
    public static UniformBuffer globalUBOBuf;
    public static ShaderStorageBuffer voxelSSBO;
    public void createBuffers(MemoryStack stack) {
        globalUBOBuf = new UniformBuffer(stack, globalUBO.size(), VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, globalUBO);
        voxelSSBO = new ShaderStorageBuffer(stack, World.size*World.height*World.size*4,VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, false);
    }

    public static void recreateDescriptors(MemoryStack stack) {if (descriptors != null) {descriptors = new Descriptors(stack);}}

    public static void rebuild() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long flags = SDL_GetWindowFlags(window);
            while ((flags & SDL_WINDOW_MINIMIZED) != 0) {
                flags = SDL_GetWindowFlags(window);
                SDL_PollEvent(events);
            }
            cleanupSwapchain();

            long vkOldSwapchain = vkSwapchain;
            Swapchain.init(stack);
            Pipeline.recreatePipeline(stack);
            SyncObjects.init(stack);
            vkDestroySwapchainKHR(vkDevice, vkOldSwapchain, null);
        }
    }

    public static void cleanupSwapchain() {
        vkDeviceWaitIdle(vkDevice);
        //sync objects
        for (int i = 0; i < FRAMES_IN_FLIGHT; i++) {
            vkDestroySemaphore(vkDevice, imageAvailableSemaphores[i], null);
            vkDestroyFence(vkDevice, inFlightFences[i], null);
        }
        for (int i = 0; i < Swapchain.images.length; i++) {
            vkDestroySemaphore(vkDevice, renderFinishedSemaphores[i], null);
        }
        //pipeline
        vkDestroyPipeline(vkDevice, graphicsPipeline, null);
        vkDestroyPipelineLayout(vkDevice, pipelineLayout, null);
        //swapchain
        for (long i : imageViews) {
            vkDestroyImageView(vkDevice, i, null);
        }
        vkDestroyImageView(vkDevice, depthImageView, null);
        vkDestroyImage(vkDevice, depthImage, null);
        vkFreeMemory(vkDevice, depthImageMemory, null);
    }
    public void cleanup() {
        SDL_DestroyWindow(window);
        SDL_Quit();
    }
}
