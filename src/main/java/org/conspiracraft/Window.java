package org.conspiracraft;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.sdl.SDLError.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLKeyboard.*;
import static org.lwjgl.sdl.SDLLog.*;
import static org.lwjgl.sdl.SDLMouse.*;
import static org.lwjgl.sdl.SDLPixels.SDL_COLORSPACE_HDR10;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.sdl.SDLVulkan.*;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_HDR10_HLG_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_EXT_SWAPCHAIN_COLOR_SPACE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class Window {
    public static long window;
    public static VkInstance vkInst;
    public static long vkSurf;
    public static int vkQueueFamilyIdx;
    public static VkPhysicalDevice physicalDevice;
    public static VkDevice device;
    public static VkQueue queue;
    public static long queueHandle;
    public static VkSurfaceFormatKHR vkSurfFormat;
    public static long swapchain;

    private int width = Settings.width;
    private int height = Settings.height;
    private final Matrix4f projectionMatrix = new Matrix4f();
    public ByteBuffer keys;

    public Window() {
        if (!SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO | SDL_COLORSPACE_HDR10)) {throw new IllegalStateException("Unable to initialize SDL");}

        try (MemoryStack stack = MemoryStack.stackPush()) {
            createVkInst(stack);
            createSDLWindow();
            createVkSurf();
            createVkPhysicalDeviceAndVkQueue(stack);
            createVkDeviceAndGraphicsQueue(stack);
            createSwapchain(stack);
        }
    }

    public void createSDLWindow() {
        window = SDL_CreateWindow(Constants.GAME_NAME, width, height, SDL_WINDOW_VULKAN | SDL_WINDOW_RESIZABLE);
        if (window == 0) {SDL_LogCritical(SDL_LOG_CATEGORY_APPLICATION, "Failed to create window: %s\n"+SDL_GetError());SDL_Quit();}

        SDL_SetWindowResizable(window, true);
        SDL_SetWindowRelativeMouseMode(window, true);
    }

    public void createSwapchain(MemoryStack stack) {
        VkSurfaceCapabilitiesKHR caps = VkSurfaceCapabilitiesKHR.malloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, vkSurf, caps);

        IntBuffer formatCount = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, vkSurf, formatCount, null);
        VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.malloc(formatCount.get(0), stack);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, vkSurf, formatCount, formats);
        for (int i = 0; i < formats.capacity(); i++) {
            VkSurfaceFormatKHR f = formats.get(i);
            System.out.println(f.format() + "  " + f.colorSpace());
        }

        IntBuffer presentCount = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, vkSurf, presentCount, null);
        IntBuffer presentModes = stack.mallocInt(presentCount.get(0));
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, vkSurf, presentCount, presentModes);

        for (int i = 0; i < formats.capacity(); i++) {
            VkSurfaceFormatKHR f = formats.get(i);
            if (f.format() == VK_FORMAT_A2R10G10B10_UNORM_PACK32 &&
                    f.colorSpace() == VK_COLOR_SPACE_HDR10_HLG_EXT) {
                vkSurfFormat = f;
                break;
            }
        }
        if (vkSurfFormat == null) {
            vkSurfFormat = formats.get(0); //fallback
        }

        int chosenPresentMode = VK_PRESENT_MODE_FIFO_KHR; // always supported
        for (int i = 0; i < presentModes.capacity(); i++) {
            int mode = presentModes.get(i);
            if (mode == VK_PRESENT_MODE_MAILBOX_KHR) {
                chosenPresentMode = mode;
                break;
            }
        }
        VkExtent2D extent = VkExtent2D.calloc(stack).width(width).height(height);
        int imageCount = 2;
        int imgFormat = vkSurfFormat.format();
        int imgColorSpace = vkSurfFormat.colorSpace();
        VkSwapchainCreateInfoKHR swapInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .surface(vkSurf)
                .minImageCount(imageCount)
                .imageFormat(imgFormat)
                .imageColorSpace(imgColorSpace)
                .imageExtent(extent)
                .imageArrayLayers(1)
                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .preTransform(caps.currentTransform())
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                .presentMode(chosenPresentMode)
                .clipped(true)
                .oldSwapchain(VK_NULL_HANDLE);
        LongBuffer pSwapchain = stack.mallocLong(1);
        int err = vkCreateSwapchainKHR(device, swapInfo, null, pSwapchain);
        if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to create swapchain: " + err);
        }
        swapchain = pSwapchain.get(0);

        IntBuffer imgCount = stack.mallocInt(1);
        vkGetSwapchainImagesKHR(device, swapchain, imgCount, null);
        LongBuffer images = stack.mallocLong(imgCount.get(0));
        vkGetSwapchainImagesKHR(device, swapchain, imgCount, images);
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
        device = new VkDevice(pDevice.get(0), physicalDevice, deviceInfo);

        // Retrieve the queue
        PointerBuffer pQueue = stack.mallocPointer(1);
        vkGetDeviceQueue(device, vkQueueFamilyIdx, 0, pQueue);
        queueHandle = pQueue.get(0);

        queue = new VkQueue(queueHandle, device);
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
            System.out.println("Device name: " + pdProperties.deviceNameString());

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
    public void createVkInst(MemoryStack stack) {
        PointerBuffer extBuffer = SDLVulkan.SDL_Vulkan_GetInstanceExtensions();
        int extCount = extBuffer.remaining();
        PointerBuffer extensions = MemoryUtil.memAllocPointer(extCount+1);
        for (int i = 0; i < extCount; i++) {
            extensions.put(i, extBuffer.get(i));
        }
        extensions.put(extCount, memUTF8(VK_EXT_SWAPCHAIN_COLOR_SPACE_EXTENSION_NAME));

        VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                .sType(VK13.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(stack.UTF8("Conspiracraft"))
                .applicationVersion(VK13.VK_MAKE_VERSION(1, 0, 0))
                .pEngineName(stack.UTF8("Conspiracraft Engine"))
                .engineVersion(VK13.VK_MAKE_VERSION(1, 0, 0))
                .apiVersion(VK13.VK_API_VERSION_1_3);

        VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                .sType(VK13.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(extensions)
                .flags(VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR);

        PointerBuffer pInstance = stack.mallocPointer(1);
        int err = VK13.vkCreateInstance(createInfo, null, pInstance);
        if (err != VK13.VK_SUCCESS) {
            throw new RuntimeException("Failed to create Vulkan instance: " + err);
        }

        vkInst = new VkInstance(pInstance.get(0), createInfo);
    }

    public void cleanup() {
        SDL_DestroyWindow(Window.window);
        SDL_Quit();
    }

    public int getHeight() {
        return height;
    }
    public int getWidth() {
        return width;
    }

    public boolean isKeyPressed(int keyCode) {
        return keys.get(keyCode) > 0;
    }

    public boolean leftButtonPressed = false;
    public boolean middleButtonPressed = false;
    public boolean rightButtonPressed = false;
    public Vector2f scroll = new Vector2f(0);
    public Vector2f displVec = new Vector2f(0);
    public Vector2f currentPos = new Vector2f(0);

    public void input() {
        leftButtonPressed = (SDL_GetMouseState(null, null)&SDL_BUTTON_LEFT) > 0;
        rightButtonPressed = (SDL_GetMouseState(null, null)&SDL_BUTTON_RIGHT) > 0;
        middleButtonPressed = (SDL_GetMouseState(null, null)&SDL_BUTTON_MIDDLE) > 0;
        keys = SDL_GetKeyboardState();
    }

    public void pollEvents(SDL_Event event) {
        displVec.x = 0;
        displVec.y = 0;
        scroll.set(0);
        while (SDL_PollEvent(event)) {
            switch (event.type()) {
                case SDL_EVENT_QUIT:
                    Main.isClosing = true;
                    break;
                case SDL_EVENT_WINDOW_RESIZED:
                    resized(event.window().data1(), event.window().data2());
                    break;
                case SDL_EVENT_MOUSE_MOTION:
                    displVec.y += event.motion().xrel();
                    displVec.x += event.motion().yrel();
                    currentPos.x = event.motion().x();
                    currentPos.y = event.motion().y();
                    break;
                case SDL_EVENT_MOUSE_WHEEL:
                    scroll.x = event.wheel().x();
                    scroll.y = event.wheel().y();
                    break;
                default:
                    break;
            }
        }
    }

    public void resized(int width, int height) {
        this.width = width;
        this.height = height;
        try {

        } catch (Exception excp) {
            Logger.error("Error calling resize callback", excp);
        }
    }

    public void update() {
        SDL_GL_SwapWindow(window);
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
    public Matrix4f updateProjectionMatrix() {
        float aspectRatio = (float) width /height;
        projectionMatrix.identity();
        projectionMatrix.set(
                1.f/Settings.fov, 0.f, 0.f, 0.f,
                0.f, aspectRatio/Settings.fov, 0.f, 0.f,
                0.f, 0.f, 0.f, -1.f,
                0.f, 0.f, Settings.Z_NEAR, 0.f
        );
        return projectionMatrix;
    }
}