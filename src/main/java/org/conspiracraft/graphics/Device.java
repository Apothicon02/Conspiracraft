package org.conspiracraft.graphics;

import org.conspiracraft.Constants;
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDLVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.conspiracraft.Settings.height;
import static org.conspiracraft.Settings.width;
import static org.conspiracraft.renderer.Window.window;
import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLEvents.SDL_PumpEvents;
import static org.lwjgl.sdl.SDLInit.SDL_Quit;
import static org.lwjgl.sdl.SDLLog.SDL_LOG_CATEGORY_APPLICATION;
import static org.lwjgl.sdl.SDLLog.SDL_LogCritical;
import static org.lwjgl.sdl.SDLMouse.SDL_SetWindowRelativeMouseMode;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowResizable;
import static org.lwjgl.sdl.SDLVulkan.SDL_Vulkan_CreateSurface;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_EXT_SWAPCHAIN_COLOR_SPACE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

public class Device {
    public static VkInstance vkInst;
    public static long vkSurf;
    public static int vkQueueFamilyIdx;
    public static VkPhysicalDevice physicalDevice;
    public static VkDevice vkDevice;
    public static VkQueue graphicsQueue;
    public static VkQueue presentQueue;
    public static long graphicsQueueHandle;
    public static long presentQueueHandle;

    public Device(MemoryStack stack) {
        createVkInst(stack);
        createSDLWindow();
        createVkSurf();
        createVkPhysicalDeviceAndVkQueue(stack);
        createVkDeviceAndGraphicsQueue(stack);
    }

    public void createVkDeviceAndGraphicsQueue(MemoryStack stack) {
        FloatBuffer priorities = stack.floats(1.0f);

        VkDeviceQueueCreateInfo.Buffer queueInfo =
                VkDeviceQueueCreateInfo.calloc(1, stack)
                        .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(vkQueueFamilyIdx)
                        .pQueuePriorities(priorities);

        PointerBuffer deviceExtensions = stack.mallocPointer(1);
        deviceExtensions.put(0, stack.UTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME));

        VkDeviceCreateInfo deviceInfo = VkDeviceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(queueInfo)
                .ppEnabledExtensionNames(deviceExtensions);

        PointerBuffer pDevice = stack.mallocPointer(1);
        int deviceCreateErr = vkCreateDevice(physicalDevice, deviceInfo, null, pDevice);
        if (deviceCreateErr != VK_SUCCESS) {
            throw new RuntimeException("Failed to create device: " + deviceCreateErr);
        }
        vkDevice = new VkDevice(pDevice.get(0), physicalDevice, deviceInfo);

        PointerBuffer graphicsQueueBuf = stack.mallocPointer(1);
        vkGetDeviceQueue(vkDevice, vkQueueFamilyIdx, 0, graphicsQueueBuf);
        graphicsQueueHandle = graphicsQueueBuf.get(0);
        graphicsQueue = new VkQueue(graphicsQueueHandle, vkDevice);

        PointerBuffer presentQueueBuf = stack.mallocPointer(1);
        vkGetDeviceQueue(vkDevice, vkQueueFamilyIdx, 0, presentQueueBuf);
        presentQueueHandle = presentQueueBuf.get(0);
        presentQueue = new VkQueue(presentQueueHandle, vkDevice);
    }
    public void createVkPhysicalDeviceAndVkQueue(MemoryStack stack) {
        IntBuffer deviceCount = stack.mallocInt(1);
        vkEnumeratePhysicalDevices(vkInst, deviceCount, null);
        if (deviceCount.get(0) == 0) {
            throw new RuntimeException("No Vulkan physical devices found");
        }

        int dGpuQueue = 0;
        VkPhysicalDevice dGpu = null;
        int iGpuQueue = 0;
        VkPhysicalDevice iGpu = null;
        int vGpuQueue = 0;
        VkPhysicalDevice vGpu = null;
        int oGpuQueue = 0;
        VkPhysicalDevice oGpu = null;
        int cpuQueue = 0;
        VkPhysicalDevice cpu = null;

        PointerBuffer devices = stack.mallocPointer(deviceCount.get(0));
        vkEnumeratePhysicalDevices(vkInst, deviceCount, devices);
        for (int d = 0; d < devices.capacity(); d++) {
            VkPhysicalDevice pDevice = new VkPhysicalDevice(devices.get(d), vkInst);
            VkPhysicalDeviceProperties pdProperties;
            pdProperties = VkPhysicalDeviceProperties.malloc(stack);
            vkGetPhysicalDeviceProperties(pDevice, pdProperties);
            //System.out.println("Device name: " + pdProperties.deviceNameString());

            IntBuffer queueCount = stack.mallocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(pDevice, queueCount, null);
            VkQueueFamilyProperties.Buffer queues = VkQueueFamilyProperties.malloc(queueCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(pDevice, queueCount, queues);
            for (int q = 0; q < queues.capacity(); q++) {
                VkQueueFamilyProperties queueProperties = queues.get(q);
                boolean graphics = (queueProperties.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0;

                IntBuffer presentSupport = stack.mallocInt(1);
                vkGetPhysicalDeviceSurfaceSupportKHR(pDevice, q, vkSurf, presentSupport);

                boolean present = presentSupport.get(0) == VK_TRUE;

                if (graphics && present) {
                    switch (pdProperties.deviceType()) {
                        case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> {dGpu = pDevice; dGpuQueue = q;}
                        case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU -> {iGpu = pDevice; iGpuQueue = q;}
                        case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU -> {vGpu = pDevice; vGpuQueue = q;}
                        case VK_PHYSICAL_DEVICE_TYPE_OTHER -> {oGpu = pDevice; oGpuQueue = q;}
                        case VK_PHYSICAL_DEVICE_TYPE_CPU -> {cpu = pDevice; cpuQueue = q;}
                    }
                    break;
                }
            }
        }
        if (dGpu != null) {vkQueueFamilyIdx = dGpuQueue;physicalDevice = dGpu;} else
        if (iGpu != null) {vkQueueFamilyIdx = iGpuQueue;physicalDevice = iGpu;} else
        if (vGpu != null) {vkQueueFamilyIdx = vGpuQueue;physicalDevice = vGpu;} else
        if (oGpu != null) {vkQueueFamilyIdx = oGpuQueue;physicalDevice = oGpu;} else
        if (cpu != null) {vkQueueFamilyIdx = cpuQueue;physicalDevice = cpu;}
    }
    public void createVkSurf() {
        LongBuffer surface = MemoryUtil.memAllocLong(1);
        if (!SDL_Vulkan_CreateSurface(window, vkInst, null, surface)) {
            throw new RuntimeException("Failed to create surface: %s\n" + SDL_GetError());
        }
        vkSurf = surface.get(0);
    }
    public void createSDLWindow() {
        window = SDLVideo.SDL_CreateWindow(Constants.GAME_NAME, width, height, SDL_WINDOW_VULKAN | SDL_WINDOW_RESIZABLE | SDL_WINDOW_HIGH_PIXEL_DENSITY);
        if (window == 0) {SDL_LogCritical(SDL_LOG_CATEGORY_APPLICATION, "Failed to create window: %s\n"+SDL_GetError());SDL_Quit();}

        SDL_SetWindowResizable(window, true);
        SDL_SetWindowRelativeMouseMode(window, true);
        SDL_PumpEvents();
    }
    public void createVkInst(MemoryStack stack) {
        PointerBuffer extBuffer = SDLVulkan.SDL_Vulkan_GetInstanceExtensions();
        int extCount = extBuffer.remaining();
        PointerBuffer extensions = MemoryUtil.memAllocPointer(extCount+2);
        for (int i = 0; i < extCount; i++) {
            extensions.put(i, extBuffer.get(i));
        }
        extensions.put(extCount, memUTF8(VK_EXT_SWAPCHAIN_COLOR_SPACE_EXTENSION_NAME));
        extensions.put(extCount+1, memUTF8(VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME));

        VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                .sType(VK13.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(stack.UTF8("Conspiracraft"))
                .applicationVersion(VK13.VK_MAKE_VERSION(1, 0, 0))
                .pEngineName(stack.UTF8("ConspirEngine"))
                .engineVersion(VK13.VK_MAKE_VERSION(1, 0, 0))
                .apiVersion(VK13.VK_API_VERSION_1_3);
        PointerBuffer layers = stack.mallocPointer(1);
        layers.put(0, stack.UTF8("VK_LAYER_KHRONOS_validation")); //disable when not in dev env
        VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                .sType(VK13.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(extensions)
                .ppEnabledLayerNames(layers)
                .flags(VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR);

        PointerBuffer pInstance = stack.mallocPointer(1);
        int err = VK13.vkCreateInstance(createInfo, null, pInstance);
        if (err != VK13.VK_SUCCESS) {
            throw new RuntimeException("Failed to create Vulkan instance: " + err);
        }

        vkInst = new VkInstance(pInstance.get(0), createInfo);
    }
}
