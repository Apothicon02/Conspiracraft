package org.conspiracraft.graphics;

import org.conspiracraft.Settings;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.conspiracraft.graphics.Device.*;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_EXTENDED_SRGB_LINEAR_EXT;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class Swapchain {
    public static VkSurfaceFormatKHR vkSurfFormat;
    public static long vkSwapchain = VK_NULL_HANDLE;
    public static long[] swapchainImages;
    public static int eWidth;
    public static int eHeight;
    public static boolean hdr = false;

    public Swapchain(MemoryStack stack) {
        createSwapchain(stack);
    }

    public void createSwapchain(MemoryStack stack) {
        VkSurfaceCapabilitiesKHR caps = VkSurfaceCapabilitiesKHR.malloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, vkSurf, caps);

        IntBuffer formatCount = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, vkSurf, formatCount, null);
        VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.malloc(formatCount.get(0), stack);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, vkSurf, formatCount, formats);

        IntBuffer presentCount = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, vkSurf, presentCount, null);
        IntBuffer presentModes = stack.mallocInt(presentCount.get(0));
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, vkSurf, presentCount, presentModes);

        vkSurfFormat = null;
        for (int i = 0; i < formats.capacity(); i++) { //prioritize hdr
            hdr = true;
            VkSurfaceFormatKHR f = formats.get(i);
            //System.out.print(f.format()+" "+f.colorSpace()+"\n");
            if (f.format() == VK_FORMAT_R16G16B16A16_SFLOAT && //VK_FORMAT_A2B10G10R10_UNORM_PACK32
                    f.colorSpace() == VK_COLOR_SPACE_EXTENDED_SRGB_LINEAR_EXT) { //VK_COLOR_SPACE_HDR10_ST2084_EXT
                vkSurfFormat = f;
                break;
            }
        }
        if (vkSurfFormat == null) { //sRGB if not hdr
            hdr = false;
            for (int i = 0; i < formats.capacity(); i++) {
                VkSurfaceFormatKHR f = formats.get(i);
                if (f.format() == VK_FORMAT_B8G8R8A8_SRGB &&
                        f.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                    vkSurfFormat = f;
                    break;
                }
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
        int imageCount = 2;
        int imgFormat = vkSurfFormat.format();
        int imgColorSpace = vkSurfFormat.colorSpace();
        VkSwapchainCreateInfoKHR swapInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .surface(vkSurf)
                .minImageCount(imageCount)
                .imageFormat(imgFormat)
                .imageColorSpace(imgColorSpace)
                .imageExtent(VkExtent2D.calloc(stack).width(Settings.width).height(Settings.height))
                .imageArrayLayers(1)
                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .preTransform(caps.currentTransform())
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                .presentMode(chosenPresentMode)
                .clipped(true)
                .oldSwapchain(vkSwapchain);
        LongBuffer pSwapchain = stack.mallocLong(1);
        int err = vkCreateSwapchainKHR(vkDevice, swapInfo, null, pSwapchain);
        if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to create swapchain: " + err);
        }
        vkSwapchain = pSwapchain.get(0);

        IntBuffer imgCount = stack.mallocInt(1);
        vkGetSwapchainImagesKHR(vkDevice, vkSwapchain, imgCount, null);
        LongBuffer images = stack.mallocLong(imgCount.get(0));
        vkGetSwapchainImagesKHR(vkDevice, vkSwapchain, imgCount, images);
        swapchainImages = new long[images.capacity()];
        images.get(swapchainImages);
        eWidth = caps.currentExtent().width();
        eHeight = caps.currentExtent().height();
    }
}
