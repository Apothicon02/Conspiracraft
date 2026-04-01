package org.conspiracraft.renderer;

import org.conspiracraft.Constants;
import org.conspiracraft.Main;
import org.conspiracraft.Settings;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.conspiracraft.Settings.*;
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
import static org.lwjgl.vulkan.EXTSwapchainColorspace.*;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME;
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
    public static VkQueue graphicsQueue;
    public static VkQueue presentQueue;
    public static long graphicsQueueHandle;
    public static long presentQueueHandle;
    public static VkSurfaceFormatKHR vkSurfFormat;
    public static long swapchain;
    public static long[] swapchainImages;
    public static long[] imageViews;
    public static long renderPass;
    public static long pipelineLayout;
    public static long graphicsPipeline;
    public static long[] swapchainFramebuffers;
    public static long commandPool;
    public static VkCommandBuffer commandBuffer;
    public static long imageAvailableSemaphore;
    public static long renderFinishedSemaphore;
    public static long inFlightFence;
    public static int eWidth;
    public static int eHeight;

    private final Matrix4f projectionMatrix = new Matrix4f();
    public ByteBuffer keys;

    public Window() {
        if (!SDL_Init(hdr ? (SDL_INIT_VIDEO | SDL_INIT_AUDIO | SDL_COLORSPACE_HDR10) : (SDL_INIT_VIDEO | SDL_INIT_AUDIO))) {throw new IllegalStateException("Unable to initialize SDL");}

        try (MemoryStack stack = MemoryStack.stackPush()) {
            createVkInst(stack);
            createSDLWindow();
            createVkSurf();
            createVkPhysicalDeviceAndVkQueue(stack);
            createVkDeviceAndGraphicsQueue(stack);
            createSwapchain(stack);
            createImageViews(stack);
            createRenderPass(stack);
            createGraphicsPipeline(stack);
            createFramebuffers(stack);
            createCommandPool(stack);
            createCommandBuffer(stack);
            createSyncObjects(stack);
        }
    }
    public void createSyncObjects(MemoryStack stack) {
        VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
        semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
        VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
        fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
        fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

        LongBuffer imageAvailableSemBuf = stack.mallocLong(1);
        LongBuffer renderFinishedSemBuf = stack.mallocLong(1);
        LongBuffer inFlightFenceBuf = stack.mallocLong(1);
        if (vkCreateSemaphore(device, semaphoreInfo, null, imageAvailableSemBuf) != VK_SUCCESS ||
                vkCreateSemaphore(device, semaphoreInfo, null, renderFinishedSemBuf) != VK_SUCCESS ||
                vkCreateFence(device, fenceInfo, null, inFlightFenceBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create semaphores!");
        }
        imageAvailableSemaphore = imageAvailableSemBuf.get(0);
        renderFinishedSemaphore = renderFinishedSemBuf.get(0);
        inFlightFence = inFlightFenceBuf.get(0);
    }
    public void createCommandBuffer(MemoryStack stack) {
        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
        allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
        allocInfo.commandPool(commandPool);
        allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
        allocInfo.commandBufferCount(1);

        PointerBuffer commandBuffersBuf = stack.mallocPointer(1);
        if (vkAllocateCommandBuffers(device, allocInfo, commandBuffersBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate command buffers!");
        }
        commandBuffer = new VkCommandBuffer(commandBuffersBuf.get(0), device);
    }
    public void createCommandPool(MemoryStack stack) {
        //QueueFamilyIndices queueFamilyIndices = QueueFamilyIndices.findQueueFamilies(physicalDevice, vkSurf);

        VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(vkQueueFamilyIdx);

        LongBuffer commandPoolBuf = stack.mallocLong(1);
        if (vkCreateCommandPool(device, poolInfo, null, commandPoolBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create command pool!");
        }
        commandPool = commandPoolBuf.get(0);
    }
    public void createFramebuffers(MemoryStack stack) {
        swapchainFramebuffers = new long[imageViews.length];
        for (int i = 0; i < imageViews.length; i++) {
            LongBuffer attachments = stack.mallocLong(1).put(imageViews[i]).flip();

            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(renderPass);
            framebufferInfo.attachmentCount(1);
            framebufferInfo.pAttachments(attachments);
            framebufferInfo.width(eWidth);
            framebufferInfo.height(eHeight);
            framebufferInfo.layers(1);

            LongBuffer framebufferBuf = stack.mallocLong(1);
            int err = vkCreateFramebuffer(device, framebufferInfo, null, framebufferBuf);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to create framebuffer: " + err);
            }
            swapchainFramebuffers[i] = framebufferBuf.get(0);
        }
    }
    public void createGraphicsPipeline(MemoryStack stack) {
        ByteBuffer vertShader = ShaderHelper.compileGLSLString(new String[]{"default.vert"}, Shaderc.shaderc_glsl_vertex_shader);
        ByteBuffer fragShader = ShaderHelper.compileGLSLString(new String[]{"default.frag"}, Shaderc.shaderc_glsl_fragment_shader);
        long vertShaderModule = ShaderHelper.createShaderModule(vertShader);
        long fragShaderModule = ShaderHelper.createShaderModule(fragShader);

        VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
        shaderStages.get(0)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(VK_SHADER_STAGE_VERTEX_BIT)
                .module(vertShaderModule)
                .pName(stack.UTF8("main"));
        shaderStages.get(1)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                .module(fragShaderModule)
                .pName(stack.UTF8("main"));

        VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                .pDynamicStates(stack.mallocInt(2).put(VK_DYNAMIC_STATE_VIEWPORT).put(VK_DYNAMIC_STATE_SCISSOR).flip());

//        VkVertexInputBindingDescription.Buffer binding =
//                VkVertexInputBindingDescription.calloc(1, stack)
//                        .binding(0)
//                        .stride(Vertex.size)
//                        .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
//
//        VkVertexInputAttributeDescription.Buffer attributes =
//                VkVertexInputAttributeDescription.calloc(3, stack);
//
//        attributes.get(0)
//                .binding(0)
//                .location(0)
//                .format(VK_FORMAT_R32G32G32_SFLOAT)
//                .offset(0);
//
//        attributes.get(1)
//                .binding(0)
//                .location(1)
//                .format(VK_FORMAT_R32G32G32_SFLOAT)
//                .offset(12);
//
//        attributes.get(2)
//                .binding(0)
//                .location(2)
//                .format(VK_FORMAT_R32G32_SFLOAT)
//                .offset(24);

        VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
        vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
        vertexInputInfo.pVertexBindingDescriptions(null);
        vertexInputInfo.pVertexAttributeDescriptions(null);

        VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
        inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
        inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
        inputAssembly.primitiveRestartEnable(false);

        VkViewport viewport = VkViewport.calloc(stack);
        viewport.x(0.0f);
        viewport.y(0.0f);
        viewport.width((float) eWidth);
        viewport.height((float) eHeight);
        viewport.minDepth(0.0f);
        viewport.maxDepth(1.0f);

        VkRect2D scissor = VkRect2D.calloc(stack);
        scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
        scissor.extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight));

        VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .viewportCount(1)
                .scissorCount(1);

        VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                .depthClampEnable(false)
                .rasterizerDiscardEnable(false)
                .polygonMode(VK_POLYGON_MODE_FILL)
                .lineWidth(1.0f)
                .cullMode(VK_CULL_MODE_BACK_BIT)
                .frontFace(VK_FRONT_FACE_CLOCKWISE)
                .depthBiasEnable(false);
//                .depthBiasConstantFactor(0.f)
//                .depthBiasClamp(0.f)
//                .depthBiasSlopeFactor(0.f);

        VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
        multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
        multisampling.sampleShadingEnable(false);
        multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);
//        multisampling.minSampleShading(1.0f); // Optional
//        multisampling.pSampleMask(null); // Optional
//        multisampling.alphaToCoverageEnable(false); // Optional
//        multisampling.alphaToOneEnable(false) // Optional

        //VkPipelineDepthStencilStateCreateInfo

        VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
        colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
        colorBlendAttachment.blendEnable(false);
//        colorBlendAttachment.srcColorBlendFactor(VK_BLEND_FACTOR_ONE); // Optional
//        colorBlendAttachment.dstColorBlendFactor(VK_BLEND_FACTOR_ZERO); // Optional
//        colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD); // Optional
//        colorBlendAttachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE); // Optional
//        colorBlendAttachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO); // Optional
//        colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD); // Optional

        VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
        colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
        colorBlending.logicOpEnable(false);
        colorBlending.logicOp(VK_LOGIC_OP_COPY); // Optional
        colorBlending.attachmentCount(1);
        colorBlending.pAttachments(colorBlendAttachment);
//        colorBlending.blendConstants(0, 0.0f); // Optional
//        colorBlending.blendConstants(1, 0.0f); // Optional
//        colorBlending.blendConstants(2, 0.0f); // Optional
//        colorBlending.blendConstants(3, 0.0f); // Optional

        VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
        pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
//        pipelineLayoutInfo.setLayoutCount(0); // Optional
//        pipelineLayoutInfo.pSetLayouts(null); // Optional
//        pipelineLayoutInfo.pPushConstantRanges(null); // Optional

        LongBuffer pPipelineLayout = stack.mallocLong(1);
        int err = vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout);
        if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to create pipeline layout: " + err);
        }
        pipelineLayout = pPipelineLayout.get(0);

        VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                .stageCount(2)
                .pStages(shaderStages)
                .pVertexInputState(vertexInputInfo)
                .pInputAssemblyState(inputAssembly)
                .pViewportState(viewportState)
                .pRasterizationState(rasterizer)
                .pMultisampleState(multisampling)
                .pDepthStencilState(null)
                .pColorBlendState(colorBlending)
                .pDynamicState(dynamicState)
                .layout(pipelineLayout)
                .renderPass(renderPass)
                .subpass(0)
                .basePipelineHandle(VK_NULL_HANDLE) // Optional
                .basePipelineIndex(-1); // Optional

        LongBuffer pipelineBuf = stack.mallocLong(1);
        if (vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pipelineBuf) != VK_SUCCESS) {
            throw new RuntimeException("failed to create graphics pipeline!");
        }
        graphicsPipeline = pipelineBuf.get(0);

        vkDestroyShaderModule(device, fragShaderModule, null);
        vkDestroyShaderModule(device, vertShaderModule, null);
    }
    public void createRenderPass(MemoryStack stack) {
        VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.calloc(1, stack);
        colorAttachment.format(vkSurfFormat.format());
        colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
        colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
        colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
        colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
        colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
        colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

        VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.calloc(1, stack);
        colorAttachmentRef.attachment(0);
        colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
        subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
        subpass.colorAttachmentCount(1);
        subpass.pColorAttachments(colorAttachmentRef);

        VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack)
                .srcSubpass(VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
        VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(colorAttachment)
                .pSubpasses(subpass)
                .pDependencies(dependency);

        LongBuffer renderPassBuf = stack.mallocLong(1);
        if (vkCreateRenderPass(device, renderPassInfo, null, renderPassBuf) != VK_SUCCESS) {
            throw new RuntimeException("failed to create render pass!");
        }
        renderPass = renderPassBuf.get(0);
    }
    public void createImageViews(MemoryStack stack) {
        imageViews = new long[swapchainImages.length];
        for (int i = 0; i < swapchainImages.length; i++) {
            VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(swapchainImages[i])
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(vkSurfFormat.format());
            createInfo.components()
                    .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1);
            LongBuffer pView = stack.mallocLong(1);
            int err = vkCreateImageView(device, createInfo, null, pView);
            if (err != VK_SUCCESS) throw new RuntimeException("Failed to create image view: " + err);
            imageViews[i] = pView.get(0);
        }
    }
    public void createSwapchain(MemoryStack stack) {
        VkSurfaceCapabilitiesKHR caps = VkSurfaceCapabilitiesKHR.malloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, vkSurf, caps);

        IntBuffer formatCount = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, vkSurf, formatCount, null);
        VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.malloc(formatCount.get(0), stack);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, vkSurf, formatCount, formats);
//        for (int i = 0; i < formats.capacity(); i++) {
//            VkSurfaceFormatKHR f = formats.get(i);
//            System.out.println(f.format() + "  " + f.colorSpace());
//        }

        IntBuffer presentCount = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, vkSurf, presentCount, null);
        IntBuffer presentModes = stack.mallocInt(presentCount.get(0));
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, vkSurf, presentCount, presentModes);

        if (hdr) {
            for (int i = 0; i < formats.capacity(); i++) {
                VkSurfaceFormatKHR f = formats.get(i);
                if (f.format() == VK_FORMAT_A2B10G10R10_UNORM_PACK32 &&
                        f.colorSpace() == VK_COLOR_SPACE_HDR10_ST2084_EXT) {
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
        swapchainImages = new long[images.capacity()];
        images.get(swapchainImages);
        eWidth = caps.currentExtent().width();
        eHeight = caps.currentExtent().height();
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

        PointerBuffer graphicsQueueBuf = stack.mallocPointer(1);
        vkGetDeviceQueue(device, vkQueueFamilyIdx, 0, graphicsQueueBuf);
        graphicsQueueHandle = graphicsQueueBuf.get(0);
        graphicsQueue = new VkQueue(graphicsQueueHandle, device);

        PointerBuffer presentQueueBuf = stack.mallocPointer(1);
        vkGetDeviceQueue(device, vkQueueFamilyIdx, 0, presentQueueBuf);
        presentQueueHandle = presentQueueBuf.get(0);
        presentQueue = new VkQueue(presentQueueHandle, device);
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
                .pEngineName(stack.UTF8("Conspiracraft Engine"))
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

    public void cleanup() {
        vkDestroySemaphore(device, imageAvailableSemaphore, null);
        vkDestroySemaphore(device, renderFinishedSemaphore, null);
        vkDestroyFence(device, inFlightFence, null);
        vkDestroyCommandPool(device, commandPool, null);
        for (int i = 0; i < swapchainFramebuffers.length; i++) {
            vkDestroyFramebuffer(device, swapchainFramebuffers[i], null);
        }
        vkDestroyPipeline(device, graphicsPipeline, null);
        vkDestroyPipelineLayout(device, pipelineLayout, null);
        vkDestroyPipelineLayout(device, pipelineLayout, null);
        vkDestroyRenderPass(device, renderPass, null);
        vkDestroyPipelineLayout(device, pipelineLayout, null);
        for (long i : imageViews) {
            vkDestroyImageView(device, i, null);
        }
        SDL_DestroyWindow(Window.window);
        SDL_Quit();
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
        Settings.width = width;
        Settings.height = height;
        try {

        } catch (Exception excp) {
            Logger.error("Error calling resize callback", excp);
        }
    }

    public void update(SDL_Event event) {
        pollEvents(event);
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
    public Matrix4f updateProjectionMatrix() {
        float aspectRatio = (float) eWidth / eHeight;
        projectionMatrix.identity();
        projectionMatrix.set(
                1.f/Settings.fov, 0.f, 0.f, 0.f,
                0.f, aspectRatio/Settings.fov, 0.f, 0.f,
                0.f, 0.f, 0.f, -1.f,
                0.f, 0.f, Constants.Z_NEAR, 0.f
        );
        return projectionMatrix;
    }
}