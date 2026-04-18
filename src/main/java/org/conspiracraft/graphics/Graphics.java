package org.conspiracraft.graphics;

import org.conspiracraft.audio.AudioController;
import org.conspiracraft.graphics.buffers.Buffer;
import org.conspiracraft.graphics.buffers.BufferHelper;
import org.conspiracraft.graphics.buffers.CmdBuffer;
import org.conspiracraft.graphics.buffers.ShaderStorageBuffer;
import org.conspiracraft.graphics.buffers.ubos.GlobalUBO;
import org.conspiracraft.graphics.buffers.ubos.UniformBuffer;
import org.conspiracraft.graphics.models.Models;
import org.conspiracraft.graphics.models.Vertex;
import org.conspiracraft.graphics.textures.Textures;
import org.lwjgl.system.MemoryStack;

import static org.conspiracraft.Main.events;
import static org.conspiracraft.Window.window;
import static org.conspiracraft.graphics.Device.*;
import static org.conspiracraft.graphics.Pipelines.*;
import static org.conspiracraft.graphics.Renderer.*;
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
            CmdBuffer.createCommandPool(stack);
            createBuffers(stack);
            Textures.generate(stack);
            descriptors = new Descriptors(stack);
            CmdBuffer.createCommandBuffer(stack);
            SyncObjects.init(stack);
        }
    }

    public static int vertexBufferOffset;
    public static int indexBufferOffset;
    public static Buffer vertexStagingBuf;
    public static Buffer indexStagingBuf;
    public static Buffer vertexBuf;
    public static Buffer indexBuf;
    public static GlobalUBO globalUBO = new GlobalUBO();
    public static UniformBuffer globalUBOBuf;
    public static ShaderStorageBuffer voxelSSBO;
    public static ShaderStorageBuffer chunkSSBO;
    public static ShaderStorageBuffer lodSSBO;
    public void createBuffers(MemoryStack stack) {
        int bufferSize = Vertex.SIZE*1000;//up to 1000 vertexes.
        vertexStagingBuf = new Buffer(stack, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, true);
        indexStagingBuf = new Buffer(stack, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, true);
        Models.loadModels(vertexStagingBuf.pointer.get(0), indexStagingBuf.pointer.get(0));
        vkUnmapMemory(vkDevice, vertexStagingBuf.memory[0]);
        vkUnmapMemory(vkDevice, indexStagingBuf.memory[0]);
        vertexBuf = new Buffer(stack, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, false);
        BufferHelper.copyBuffer(stack, vertexStagingBuf.buffer[0], vertexBuf.buffer[0], bufferSize);
        indexBuf = new Buffer(stack, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, false);
        BufferHelper.copyBuffer(stack, indexStagingBuf.buffer[0], indexBuf.buffer[0], bufferSize);

        globalUBOBuf = new UniformBuffer(stack, globalUBO.size(), VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, globalUBO);
        chunkSSBO = new ShaderStorageBuffer(stack, chunkSSBOSize,VK_SHADER_STAGE_FRAGMENT_BIT, false);
        voxelSSBO = new ShaderStorageBuffer(stack, voxelSSBOSize, VK_SHADER_STAGE_FRAGMENT_BIT, false);
        lodSSBO = new ShaderStorageBuffer(stack, lodSSBOSize, VK_SHADER_STAGE_FRAGMENT_BIT, false);
    }

    public static void recreateDescriptors(MemoryStack stack) {if (descriptors != null) {descriptors = new Descriptors(stack);}}

    public static void rebuild() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long flags = SDL_GetWindowFlags(window);
            while ((flags & SDL_WINDOW_MINIMIZED) != 0) {
                flags = SDL_GetWindowFlags(window);
                SDL_PollEvent(events);
            }
            vkDeviceWaitIdle(vkDevice);
            Swapchain.recreate(stack);
            Pipelines.recreatePipeline(stack);
            Textures.resize(stack);
            recreateDescriptors(stack);
            CmdBuffer.recreate(stack);
            SyncObjects.init(stack);
            Renderer.frameIdx = 0;
            firstImages = true;
        }
    }

    public static void cleanupSwapchain() {
        //sync objects
        for (int i = 0; i < FRAMES_IN_FLIGHT; i++) {
            vkDestroySemaphore(vkDevice, imageAvailableSemaphores[i], null);
            vkDestroyFence(vkDevice, inFlightFences[i], null);
        }
        for (int i = 0; i < Swapchain.images.length; i++) {
            vkDestroySemaphore(vkDevice, renderFinishedSemaphores[i], null);
        }
        //pipeline
        for (Pipeline pipeline : pipelines) {
            vkDestroyPipeline(vkDevice, pipeline.vkPipeline, null);
        }
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
        AudioController.cleanup();
        SDL_DestroyWindow(window);
        SDL_Quit();
    }
}
