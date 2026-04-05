package org.conspiracraft.graphics;

import org.conspiracraft.renderer.ShaderHelper;
import org.conspiracraft.renderer.Window;
import org.conspiracraft.renderer.buffers.BufferHelper;
import org.conspiracraft.renderer.buffers.DefaultUBO;
import org.conspiracraft.renderer.models.Models;
import org.conspiracraft.renderer.models.Vertex;
import org.conspiracraft.renderer.textures.TextureHelper;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.conspiracraft.Main.events;
import static org.conspiracraft.graphics.Device.*;
import static org.conspiracraft.graphics.Swapchain.*;
import static org.conspiracraft.renderer.Window.window;
import static org.lwjgl.sdl.SDLEvents.SDL_PollEvent;
import static org.lwjgl.sdl.SDLInit.SDL_Quit;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class Graphics {
    public static Device device;
    public static Swapchain swapchain;
    
    public Graphics() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            device = new Device(stack);
            swapchain = new Swapchain(stack);
            createImageViews(stack);
            createRenderPass(stack);
            createDescriptorSetLayout(stack);
            createGraphicsPipeline(stack);
            createDepthResources(stack);
            createTextureImage(stack);
            createFramebuffers(stack);
            createCommandPool(stack);
            createVertexAndIndexBuffers(stack);
            createInstanceBuffer(stack);
            createUniformBuffers(stack);
            createDescriptorPool(stack);
            createDescriptorSets(stack);
            createCommandBuffers(stack);
            createSyncObjects(stack);
        }
    }
    
    public static int MAX_FRAMES_IN_FLIGHT = 2;
    public static long[] imageViews;
    public static long renderPass;
    public static long[] descriptorSetLayouts;
    public static long pipelineLayout;
    public static long graphicsPipeline;
    public static long[] swapchainFramebuffers;
    public static long commandPool;
    public static VkCommandBuffer[] commandBuffers;
    public static long[] imageAvailableSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
    public static long[] renderFinishedSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
    public static long[] inFlightFences = new long[MAX_FRAMES_IN_FLIGHT];
    public static long[] vertexBuffer;
    public static long[] vertexBufferMemory;
    public static int vertexBufferOffset;
    public static long[] indexBuffer;
    public static long[] indexBufferMemory;
    public static int indexBufferOffset;
    public static long[] instanceStagingBuffers;
    public static long[] instanceStagingBuffersMemory;
    public static long[] instanceBuffers;
    public static long[] instanceBuffersMemory;
    public static long[] uniformBuffers;
    public static long[] uniformBuffersMemory;
    public static PointerBuffer[] uniformBuffersMapped;
    public static long descriptorPool;
    public static long[] descriptorSets;

    public void createSyncObjects(MemoryStack stack) {
        VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
        semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
        VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
        fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
        fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            LongBuffer imageAvailableSemBuf = stack.mallocLong(1);
            LongBuffer renderFinishedSemBuf = stack.mallocLong(1);
            LongBuffer inFlightFenceBuf = stack.mallocLong(1);
            if (vkCreateSemaphore(vkDevice, semaphoreInfo, null, imageAvailableSemBuf) != VK_SUCCESS ||
                    vkCreateSemaphore(vkDevice, semaphoreInfo, null, renderFinishedSemBuf) != VK_SUCCESS ||
                    vkCreateFence(vkDevice, fenceInfo, null, inFlightFenceBuf) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create semaphores!");
            }
            imageAvailableSemaphores[i] = imageAvailableSemBuf.get(0);
            renderFinishedSemaphores[i] = renderFinishedSemBuf.get(0);
            inFlightFences[i] = inFlightFenceBuf.get(0);
        }
    }
    public void createCommandBuffers(MemoryStack stack) {
        commandBuffers = new VkCommandBuffer[MAX_FRAMES_IN_FLIGHT];

        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
        allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
        allocInfo.commandPool(commandPool);
        allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
        allocInfo.commandBufferCount(MAX_FRAMES_IN_FLIGHT);

        PointerBuffer commandBuffersBuf = stack.mallocPointer(MAX_FRAMES_IN_FLIGHT);
        if (vkAllocateCommandBuffers(vkDevice, allocInfo, commandBuffersBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate command buffers!");
        }
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            commandBuffers[i] = new VkCommandBuffer(commandBuffersBuf.get(i), vkDevice);
        }
    }
    public static DefaultUBO defaultUBO = new DefaultUBO();
    public void createDescriptorSets(MemoryStack stack) {
        LongBuffer layouts = stack.mallocLong(MAX_FRAMES_IN_FLIGHT);
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            layouts.put(i, descriptorSetLayouts[0]);
        }
        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(descriptorPool)
                .pSetLayouts(layouts);
        LongBuffer descriptorSetsBuf = stack.mallocLong(MAX_FRAMES_IN_FLIGHT);
        if (vkAllocateDescriptorSets(vkDevice, allocInfo, descriptorSetsBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate descriptor sets!");
        }
        descriptorSets = new long[MAX_FRAMES_IN_FLIGHT];
        for (int i = 0; i < descriptorSets.length; i++) {
            descriptorSets[i] = descriptorSetsBuf.get(i);
            VkDescriptorBufferInfo.Buffer uboInfo = VkDescriptorBufferInfo.calloc(1, stack)
                    .buffer(uniformBuffers[i])
                    .offset(0)
                    .range(defaultUBO.size());
            VkDescriptorBufferInfo.Buffer instanceBufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
                    .buffer(instanceBuffers[i])
                    .offset(0)
                    .range(instanceBufferSize);
            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(2, stack);
            descriptorWrites.get(0)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSets[i])
                    .dstBinding(0)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .pBufferInfo(uboInfo);
            descriptorWrites.get(1)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSets[i])
                    .dstBinding(1)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .descriptorCount(1)
                    .pBufferInfo(instanceBufferInfo);
            vkUpdateDescriptorSets(vkDevice, descriptorWrites, null);
        }
    }
    public void createDescriptorPool(MemoryStack stack) {
        VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);
        poolSizes.get(0)
                .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(MAX_FRAMES_IN_FLIGHT);
        poolSizes.get(1)
                .type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                .descriptorCount(MAX_FRAMES_IN_FLIGHT);
        VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .pPoolSizes(poolSizes)
                .maxSets(MAX_FRAMES_IN_FLIGHT);
        LongBuffer descriptorPoolBuf = stack.mallocLong(1);
        if (vkCreateDescriptorPool(vkDevice, poolInfo, null, descriptorPoolBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create descriptor pool!");
        }
        descriptorPool = descriptorPoolBuf.get(0);
    }
    public void createUniformBuffers(MemoryStack stack) {
        int bufferSize = defaultUBO.size();
        uniformBuffers = new long[MAX_FRAMES_IN_FLIGHT];
        uniformBuffersMemory = new long[MAX_FRAMES_IN_FLIGHT];
        uniformBuffersMapped = new PointerBuffer[MAX_FRAMES_IN_FLIGHT];
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            uniformBuffersMapped[i] = PointerBuffer.allocateDirect(1);
            createBuffer(stack, bufferSize, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, uniformBuffers, uniformBuffersMemory, i);
            vkMapMemory(vkDevice, uniformBuffersMemory[i], 0, bufferSize, 0, uniformBuffersMapped[i]);
        }
    }
    public void createVertexAndIndexBuffers(MemoryStack stack) {
        int bufferSize = Vertex.SIZE*1000;//up to 1000 vertexes.
        long[] vertexStagingBuffer = new long[1];
        long[] vertexStagingBufferMemory = new long[1];
        createBuffer(stack, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, vertexStagingBuffer, vertexStagingBufferMemory, 0);
        PointerBuffer vertexStagingPointerBuf = stack.mallocPointer(1);
        vkMapMemory(vkDevice, vertexStagingBufferMemory[0], 0, bufferSize, 0, vertexStagingPointerBuf);
        long[] indexStagingBuffer = new long[1];
        long[] indexStagingBufferMemory = new long[1];
        createBuffer(stack, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, indexStagingBuffer, indexStagingBufferMemory, 0);
        PointerBuffer indexStagingPointerBuf = stack.mallocPointer(1);
        vkMapMemory(vkDevice, indexStagingBufferMemory[0], 0, bufferSize, 0, indexStagingPointerBuf);
        Models.loadModels(vertexStagingPointerBuf.get(0), indexStagingPointerBuf.get(0));
        vkUnmapMemory(vkDevice, vertexStagingBufferMemory[0]);
        vkUnmapMemory(vkDevice, indexStagingBufferMemory[0]);

        vertexBuffer = new long[1];
        vertexBufferMemory = new long[1];
        createBuffer(stack, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, vertexBuffer, vertexBufferMemory, 0);
        BufferHelper.copyBuffer(stack, vertexStagingBuffer[0], vertexBuffer[0], bufferSize);
        indexBuffer = new long[1];
        indexBufferMemory = new long[1];
        createBuffer(stack, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, indexBuffer, indexBufferMemory, 0);
        BufferHelper.copyBuffer(stack, indexStagingBuffer[0], indexBuffer[0], bufferSize);
    }
    public int instanceBufferSize = 350000000;
    public long[] instanceStagingBufMemPointer;
    public void createInstanceBuffer(MemoryStack stack) {
        instanceStagingBuffers = new long[MAX_FRAMES_IN_FLIGHT];
        instanceStagingBuffersMemory = new long[MAX_FRAMES_IN_FLIGHT];
        instanceStagingBufMemPointer = new long[MAX_FRAMES_IN_FLIGHT];
        instanceBuffers = new long[MAX_FRAMES_IN_FLIGHT];
        instanceBuffersMemory = new long[MAX_FRAMES_IN_FLIGHT];
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            createBuffer(stack, instanceBufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, instanceStagingBuffers, instanceStagingBuffersMemory, i);
            PointerBuffer pointerBuf = stack.mallocPointer(1);
            vkMapMemory(vkDevice, instanceStagingBuffersMemory[i], 0, instanceBufferSize, 0, pointerBuf);
            instanceStagingBufMemPointer[i] = pointerBuf.get(0);
            createBuffer(stack, instanceBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, instanceBuffers, instanceBuffersMemory, i);
        }
    }
    public int findMemoryType(MemoryStack stack, int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);
        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0) {
                if ((memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                    return i;
                }
            }
        }
        throw new RuntimeException("Failed to find suitable memory type!");
    }
    public void createTextureImage(MemoryStack stack) {

    }
    public int depthFormat = VK_FORMAT_D32_SFLOAT;
    public long depthImageView;
    public LongBuffer depthImage;
    public LongBuffer depthImageMemory;
    public void createDepthResources(MemoryStack stack) {
        depthImage = stack.mallocLong(1);
        depthImageMemory = stack.mallocLong(1);
        TextureHelper.createImage(stack, eWidth, eHeight, depthFormat, VK_IMAGE_TILING_OPTIMAL, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, depthImage, depthImageMemory);
        depthImageView = TextureHelper.createImageView(stack, depthImage.get(0), depthFormat, VK_IMAGE_ASPECT_DEPTH_BIT);
    }
    public void createCommandPool(MemoryStack stack) {
        //QueueFamilyIndices queueFamilyIndices = QueueFamilyIndices.findQueueFamilies(physicalDevice, vkSurf);

        VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(vkQueueFamilyIdx);

        LongBuffer commandPoolBuf = stack.mallocLong(1);
        if (vkCreateCommandPool(vkDevice, poolInfo, null, commandPoolBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create command pool!");
        }
        commandPool = commandPoolBuf.get(0);
    }
    public void createFramebuffers(MemoryStack stack) {
        swapchainFramebuffers = new long[imageViews.length];
        for (int i = 0; i < imageViews.length; i++) {
            LongBuffer attachments = stack.mallocLong(2).put(imageViews[i]).put(depthImageView).flip();

            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(renderPass);
            framebufferInfo.attachmentCount(2);
            framebufferInfo.pAttachments(attachments);
            framebufferInfo.width(eWidth);
            framebufferInfo.height(eHeight);
            framebufferInfo.layers(1);

            LongBuffer framebufferBuf = stack.mallocLong(1);
            int err = vkCreateFramebuffer(vkDevice, framebufferInfo, null, framebufferBuf);
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
        vertexInputInfo.pVertexBindingDescriptions(Vertex.getBindingDescription());
        vertexInputInfo.pVertexAttributeDescriptions(Vertex.getAttributeDescriptions());

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
                .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
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

        VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .logicOpEnable(false)
                .logicOp(VK_LOGIC_OP_COPY)
                .attachmentCount(1)
                .pAttachments(colorBlendAttachment);
//        colorBlending.blendConstants(0, 0.0f); // Optional
//        colorBlending.blendConstants(1, 0.0f); // Optional
//        colorBlending.blendConstants(2, 0.0f); // Optional
//        colorBlending.blendConstants(3, 0.0f); // Optional

        VkPushConstantRange.Buffer pushConstRanges = VkPushConstantRange.calloc(1, stack)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .offset(0)
                .size(defaultUBO.size());
        VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .setLayoutCount(descriptorSetLayouts.length)
                .pSetLayouts(stack.longs(descriptorSetLayouts))
                .pPushConstantRanges(pushConstRanges);

        LongBuffer pPipelineLayout = stack.mallocLong(1);
        int err = vkCreatePipelineLayout(vkDevice, pipelineLayoutInfo, null, pPipelineLayout);
        if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to create pipeline layout: " + err);
        }
        pipelineLayout = pPipelineLayout.get(0);

        VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                .depthTestEnable(true)
                .depthWriteEnable(true)
                .depthCompareOp(VK_COMPARE_OP_GREATER)
                .depthBoundsTestEnable(false)
                .stencilTestEnable(false);
        VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                .stageCount(2)
                .pStages(shaderStages)
                .pVertexInputState(vertexInputInfo)
                .pInputAssemblyState(inputAssembly)
                .pViewportState(viewportState)
                .pRasterizationState(rasterizer)
                .pMultisampleState(multisampling)
                .pDepthStencilState(depthStencil)
                .pColorBlendState(colorBlending)
                .pDynamicState(dynamicState)
                .layout(pipelineLayout)
                .renderPass(renderPass)
                .subpass(0)
                .basePipelineHandle(VK_NULL_HANDLE) // Optional
                .basePipelineIndex(-1); // Optional

        LongBuffer pipelineBuf = stack.mallocLong(1);
        if (vkCreateGraphicsPipelines(vkDevice, VK_NULL_HANDLE, pipelineInfo, null, pipelineBuf) != VK_SUCCESS) {
            throw new RuntimeException("failed to create graphics pipeline!");
        }
        graphicsPipeline = pipelineBuf.get(0);

        vkDestroyShaderModule(vkDevice, fragShaderModule, null);
        vkDestroyShaderModule(vkDevice, vertShaderModule, null);
    }
    public void createDescriptorSetLayout(MemoryStack stack) {
        VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(2, stack);
        layoutBindings.get(0)
                .binding(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(1)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT);
        layoutBindings.get(1)
                .binding(1)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                .descriptorCount(1)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

        VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(layoutBindings);
        LongBuffer descriptorSetLayoutsBuf = stack.callocLong(1);
        if (vkCreateDescriptorSetLayout(vkDevice, layoutInfo, null, descriptorSetLayoutsBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create descriptor set layout!");
        }
        descriptorSetLayouts = new long[]{descriptorSetLayoutsBuf.get(0)};
    }
    public void createRenderPass(MemoryStack stack) {
        VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(2, stack);
        attachments.get(0) //color
                .format(vkSurfFormat.format())
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
        attachments.get(1) //depth
                .format(depthFormat)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

        VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.calloc(1, stack)
                .attachment(0)
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        VkAttachmentReference depthAttachmentRef = VkAttachmentReference.calloc(stack)
                .attachment(1)
                .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
        VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack)
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(1)
                .pColorAttachments(colorAttachmentRef)
                .pDepthStencilAttachment(depthAttachmentRef);

        VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack)
                .srcSubpass(VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
        VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(attachments)
                .pSubpasses(subpass)
                .pDependencies(dependency);

        LongBuffer renderPassBuf = stack.mallocLong(1);
        if (vkCreateRenderPass(vkDevice, renderPassInfo, null, renderPassBuf) != VK_SUCCESS) {
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
            int err = vkCreateImageView(vkDevice, createInfo, null, pView);
            if (err != VK_SUCCESS) throw new RuntimeException("Failed to create image view: " + err);
            imageViews[i] = pView.get(0);
        }
    }

    public void recreateSwapchain() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long flags = SDL_GetWindowFlags(window);
            while ((flags & SDL_WINDOW_MINIMIZED) != 0) {
                flags = SDL_GetWindowFlags(window);
                SDL_PollEvent(events);
            }
            cleanupSwapchain();

            swapchain.createSwapchain(stack);
            createImageViews(stack);
            createRenderPass(stack);
            createGraphicsPipeline(stack);
            createDepthResources(stack);
            createFramebuffers(stack);
            createSyncObjects(stack);
        }
    }

    public void createBuffer(MemoryStack stack, int bufferSize, int usage, int properties, long[] buffer, long[] memory, int i) {
        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(bufferSize)
                .usage(usage)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
        LongBuffer bufferBuf = stack.mallocLong(1);
        if (vkCreateBuffer(vkDevice, bufferInfo, null, bufferBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create vertex buffer!");
        }
        buffer[i] = bufferBuf.get(0);
        VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
        vkGetBufferMemoryRequirements(vkDevice, buffer[i], memRequirements);

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memRequirements.size())
                .memoryTypeIndex(findMemoryType(stack, memRequirements.memoryTypeBits(), properties));
        LongBuffer bufferMemoryBuf = stack.mallocLong(1);
        if (vkAllocateMemory(vkDevice, allocInfo, null, bufferMemoryBuf) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate buffer memory!");
        }
        memory[i] = bufferMemoryBuf.get(0);
        if (vkBindBufferMemory(vkDevice, buffer[i], memory[i], 0) != VK_SUCCESS) {
            throw new RuntimeException("Failed to bind buffer memory!");
        }
    }

    public void cleanupSwapchain() {
        vkDeviceWaitIdle(vkDevice);
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            vkDestroySemaphore(vkDevice, imageAvailableSemaphores[i], null);
            vkDestroySemaphore(vkDevice, renderFinishedSemaphores[i], null);
            vkDestroyFence(vkDevice, inFlightFences[i], null);
        }
        for (int i = 0; i < swapchainFramebuffers.length; i++) {
            vkDestroyFramebuffer(vkDevice, swapchainFramebuffers[i], null);
        }
        vkDestroyRenderPass(vkDevice, renderPass, null);
        vkDestroyPipeline(vkDevice, graphicsPipeline, null);
        vkDestroyPipelineLayout(vkDevice, pipelineLayout, null);
        for (long i : imageViews) {
            vkDestroyImageView(vkDevice, i, null);
        }
        vkDestroyImageView(vkDevice, depthImageView, null);
        vkDestroyImage(vkDevice, depthImage.get(0), null);
        vkFreeMemory(vkDevice, depthImageMemory.get(0), null);
    }
    public void cleanup() {
        cleanupSwapchain();
        vkDestroySwapchainKHR(vkDevice, swapchain.vkSwapchain, null);
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            vkDestroyBuffer(vkDevice, uniformBuffers[i], null);
            vkFreeMemory(vkDevice, uniformBuffersMemory[i], null);
        }
        vkDestroyDescriptorPool(vkDevice, descriptorPool, null);
        vkDestroyDescriptorSetLayout(vkDevice, descriptorSetLayouts[0], null);
        vkDestroyBuffer(vkDevice, vertexBuffer[0], null);
        vkFreeMemory(vkDevice, vertexBufferMemory[0], null);
        vkDestroyBuffer(vkDevice, instanceBuffers[0], null);
        vkFreeMemory(vkDevice, instanceBuffersMemory[0], null);
        vkDestroyCommandPool(vkDevice, commandPool, null);
        SDL_DestroyWindow(window);
        SDL_Quit();
    }
}
