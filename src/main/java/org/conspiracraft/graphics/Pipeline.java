package org.conspiracraft.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.conspiracraft.graphics.Device.vkDevice;
import static org.conspiracraft.graphics.Swapchain.*;
import static org.lwjgl.vulkan.VK14.*;

public class Pipeline {
    public static void init(MemoryStack stack) {
        createPipeline(stack);
    }

    public static long pipelineLayout;
    public static long graphicsPipeline;
    public static void createPipeline(MemoryStack stack) {
        long vertShaderModule = ShaderHelper.createShaderModule(ShaderHelper.compileGLSLString(new String[]{"test.vert"}, Shaderc.shaderc_glsl_vertex_shader));
        long fragShaderModule = ShaderHelper.createShaderModule(ShaderHelper.compileGLSLString(new String[]{"test.frag"}, Shaderc.shaderc_glsl_fragment_shader));
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

        VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
//        .pVertexBindingDescriptions(Vertex.getBindingDescription())
//        .pVertexAttributeDescriptions(Vertex.getAttributeDescriptions());

        VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                .primitiveRestartEnable(false);

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

        VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
        multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
        multisampling.sampleShadingEnable(false);
        multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

        VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
        colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
        colorBlendAttachment.blendEnable(false);

        VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .logicOpEnable(false)
                .logicOp(VK_LOGIC_OP_COPY)
                .attachmentCount(1)
                .pAttachments(colorBlendAttachment);

//        VkPushConstantRange.Buffer pushConstRanges = VkPushConstantRange.calloc(1, stack)
//                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
//                .offset(0);
//                .size(defaultUBO.size());
        VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
//                .setLayoutCount(Descriptors.descriptorSetLayouts.length)
//                .pSetLayouts(stack.longs(Descriptors.descriptorSetLayouts))
//                .pPushConstantRanges(pushConstRanges);

        LongBuffer pPipelineLayout = stack.mallocLong(1);
        int err = vkCreatePipelineLayout(vkDevice, pipelineLayoutInfo, null, pPipelineLayout);
        if (err != VK_SUCCESS) {throw new RuntimeException("Failed to create pipeline layout: " + err);}
        pipelineLayout = pPipelineLayout.get(0);

        VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                .depthTestEnable(true)
                .depthWriteEnable(true)
                .depthCompareOp(VK_COMPARE_OP_GREATER)
                .depthBoundsTestEnable(false)
                .stencilTestEnable(false);
        VkPipelineRenderingCreateInfo renderingInfo = VkPipelineRenderingCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO)
                .colorAttachmentCount(1)
                .pColorAttachmentFormats(stack.ints(vkSurfFormat.format()))
                .depthAttachmentFormat(VK_FORMAT_D32_SFLOAT);
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
        if (vkCreateGraphicsPipelines(vkDevice, VK_NULL_HANDLE, pipelineInfo, null, pipelineBuf) != VK_SUCCESS) {throw new RuntimeException("failed to create graphics pipeline!");}
        graphicsPipeline = pipelineBuf.get(0);

        vkDestroyShaderModule(vkDevice, fragShaderModule, null);
        vkDestroyShaderModule(vkDevice, vertShaderModule, null);
    }
}
