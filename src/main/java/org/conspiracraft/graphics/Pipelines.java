package org.conspiracraft.graphics;

import org.conspiracraft.graphics.models.Vertex;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.conspiracraft.graphics.Graphics.globalUBO;
import static org.conspiracraft.graphics.Swapchain.*;
import static org.lwjgl.vulkan.VK14.*;

public class Pipelines {
    public static long pipelineLayout;
    public static Pipeline[] pipelines;
    private static ExecutorService pool;
    public static void init(MemoryStack stack) {
        pipelines = new Pipeline[]{
                new Pipeline("fullscreen.vert", "present.frag", 1),
                new Pipeline("gui.vert", "gui.frag", 1), new Pipeline("fullscreen.vert", "ssao.frag", 1),
                new Pipeline("fullscreen.vert", "dda.frag", 2), new Pipeline("raster.vert", "raster.frag", 2)};
        pool = Executors.newFixedThreadPool(Math.min(1+pipelines.length, Runtime.getRuntime().availableProcessors()));
        pool.submit(() -> createPipelineCache(stack));
        for (Pipeline pipeline : pipelines) {pool.submit(pipeline::compile);}
        pool.shutdown();
    }
    public static long pipelineCache;
    public static void createPipelineCache(MemoryStack stack) {
        VkPipelineCacheCreateInfo cacheInfo = VkPipelineCacheCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO)
                .pInitialData(null);
        LongBuffer pCache = stack.mallocLong(1);
        int err = vkCreatePipelineCache(vkDevice, cacheInfo, null, pCache);
        if (err != VK_SUCCESS) {throw new RuntimeException("Failed to create pipeline cache: " + err);}
        pipelineCache = pCache.get(0);
    }
    public static void createPipeline(MemoryStack stack) {
        if (!pool.isTerminated()) {try {pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);} catch (InterruptedException e) {throw new RuntimeException(e);}}

        VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                .pDynamicStates(stack.mallocInt(2).put(VK_DYNAMIC_STATE_VIEWPORT).put(VK_DYNAMIC_STATE_SCISSOR).flip());

        VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(Vertex.getBindingDescription())
                .pVertexAttributeDescriptions(Vertex.getAttributeDescriptions());

        VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                .primitiveRestartEnable(false);

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
                .cullMode(VK_CULL_MODE_NONE)
                .frontFace(VK_FRONT_FACE_CLOCKWISE)
                .depthBiasEnable(false);

        VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
        multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
        multisampling.sampleShadingEnable(false);
        multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

        VkPushConstantRange.Buffer pushConstRanges = VkPushConstantRange.calloc(1, stack)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(0)
                .size(globalUBO.size());
        VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .setLayoutCount(Descriptors.descriptorSetLayouts.length)
                .pSetLayouts(stack.longs(Descriptors.descriptorSetLayouts))
                .pPushConstantRanges(pushConstRanges);

        LongBuffer pPipelineLayout = stack.mallocLong(1);
        int err = vkCreatePipelineLayout(vkDevice, pipelineLayoutInfo, null, pPipelineLayout);
        if (err != VK_SUCCESS) {throw new RuntimeException("Failed to create pipeline layout: " + err);}
        pipelineLayout = pPipelineLayout.get(0);

        VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                .depthTestEnable(true)
                .depthWriteEnable(true)
                .depthCompareOp(VK_COMPARE_OP_GREATER_OR_EQUAL)
                .depthBoundsTestEnable(false)
                .stencilTestEnable(false);

        for (int i = 0; i < pipelines.length; i++) {
            Pipeline pipeline = pipelines[i];
            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachments = VkPipelineColorBlendAttachmentState.calloc(pipeline.colorAttachments, stack);
            IntBuffer formats = stack.callocInt(pipeline.colorAttachments);
            for (int f = 0; f < formats.limit(); f++) {
                formats.put(f, vkSurfFormat.format());
                colorBlendAttachments.get(f)
                    .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                    .blendEnable(false);
            }
            VkPipelineRenderingCreateInfo renderingInfo = VkPipelineRenderingCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO)
                    .colorAttachmentCount(pipeline.colorAttachments)
                    .pColorAttachmentFormats(formats)
                    .depthAttachmentFormat(VK_FORMAT_D32_SFLOAT);
            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(false)
                    .logicOp(VK_LOGIC_OP_COPY)
                    .attachmentCount(pipeline.colorAttachments)
                    .pAttachments(colorBlendAttachments);
            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
            shaderStages.get(0)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_VERTEX_BIT)
                    .module(pipeline.vert)
                    .pName(stack.UTF8("main"));
            shaderStages.get(1)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(pipeline.frag)
                    .pName(stack.UTF8("main"));
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
                    .renderPass(VK_NULL_HANDLE)
                    .pNext(renderingInfo)
                    .subpass(0)
                    .basePipelineHandle(VK_NULL_HANDLE) // Optional
                    .basePipelineIndex(-1); // Optional

            LongBuffer pipelineBuf = stack.mallocLong(1);
            if (vkCreateGraphicsPipelines(vkDevice, pipelineCache, pipelineInfo, null, pipelineBuf) != VK_SUCCESS) {
                throw new RuntimeException("failed to create graphics pipeline!");
            }
            pipeline.vkPipeline = pipelineBuf.get(0);
        }
    }
}
