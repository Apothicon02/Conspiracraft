package org.conspiracraft.graphics;

import org.conspiracraft.gui.GUI;
import org.conspiracraft.Main;
import org.conspiracraft.graphics.buffers.ubos.PushUBO;
import org.conspiracraft.graphics.models.Index;
import org.conspiracraft.graphics.models.Models;
import org.conspiracraft.graphics.models.Vertex;
import org.conspiracraft.graphics.textures.Texture;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.graphics.buffers.CmdBufferHelper;
import org.conspiracraft.graphics.textures.ImageHelper;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.world.World;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;
import org.lwjgl.vulkan.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.Math;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.conspiracraft.Main.player;
import static org.conspiracraft.graphics.Graphics.*;
import static org.conspiracraft.graphics.Graphics.indexBuf;
import static org.conspiracraft.graphics.Pipelines.*;
import static org.conspiracraft.graphics.buffers.CmdBuffer.cmdBuffers;
import static org.conspiracraft.graphics.Device.*;
import static org.conspiracraft.graphics.Swapchain.*;
import static org.conspiracraft.graphics.SyncObjects.*;
import static org.conspiracraft.world.Earth.sunPos;
import static org.conspiracraft.world.World.*;
import static org.lwjgl.util.vma.Vma.vmaCreateVirtualBlock;
import static org.lwjgl.util.vma.Vma.vmaVirtualAllocate;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK14.*;

public class Renderer {
    public static int imageIdx = 0;
    public static int frameIdx = 0;
    public static boolean firstImages = true;
    public static boolean initialized = false;
    public static boolean clearedDepth = false;
    public static VkCommandBuffer currentCmdBuffer;
    public static PushUBO pushUBO = new PushUBO();
    public static Pipeline currentPipeline;
    public static List<Matrix4f> cubes = new ArrayList<>();
    public static void render() throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (startCommandBuffers(stack)) {
                clearedDepth = false;
                if (!initialized) {
                    prepareTestScene();
                    BufferedImage atlasImage = Utils.loadImage("generic/texture/atlas");
//                    for (int x = 0; x < Textures.atlas.width; x++) {
//                        for (int y = 0; y < 1024; y++) {
//                            Color color = new Color(atlasImage.getRGB(x, y), true);
//                            collisionData[(x * 1024) + y] = color.getAlpha() != 0;
//                        }
//                    }
                    ImageHelper.fillImage(stack, Textures.atlas, Utils.imageToBuffer(atlasImage));
                    ImageHelper.fillImage(stack, Textures.noises, Utils.imageToBuffer(Utils.loadImage("generic/texture/coherent_noise")));
                    GUI.fillTexture();
                    initialized = true;
                } else {
//                    long basePtr = Graphics.voxelSSBO.stagingBuffer.pointer.get(0);
//                    long offsetSize = (packPos(512, 196, 512)*4L);
//                    long offsetPtr = basePtr+offsetSize;
//                    VkBufferCopy.Buffer bufferCopy = VkBufferCopy.calloc(1).srcOffset(offsetSize).dstOffset(offsetSize).size(4);
//                    MemoryUtil.memPutInt(offsetPtr, 2);
//                    vkCmdCopyBuffer(currentCmdBuffer, Graphics.voxelSSBO.stagingBuffer.buffer[0], Graphics.voxelSSBO.buffer.buffer[0], bufferCopy);
//                    VkBufferMemoryBarrier.Buffer barrierBuf = VkBufferMemoryBarrier.calloc(1)
//                            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
//                            .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
//                            .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
//                            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
//                            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
//                            .buffer(Graphics.voxelSSBO.buffer.buffer[0])
//                            .offset(0).size(defaultSSBOSize);
//                    vkCmdPipelineBarrier(currentCmdBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_VERTEX_SHADER_BIT, 0, null, barrierBuf, null);
                }
                vkCmdBindDescriptorSets(currentCmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, stack.longs(Descriptors.descriptorSets[frameIdx]), null);
                globalUBO.update(stack);
                globalUBO.submit();

                drawRaster(stack);
                drawDDA(stack);
                drawSSAO(stack);
                drawGUI(stack);

                bindPresentImage(stack);
                vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
                unbindPresentImage(stack);

                submitCommandBuffers(stack);

                frameIdx++;
                if (imageIdx >= Swapchain.images.length-1) {
                    firstImages = false;
                }
                if (frameIdx >= FRAMES_IN_FLIGHT) {frameIdx = 0;}
            }
        }
    }

    public static void drawRaster(MemoryStack stack){
        currentPipeline = pipelines[4];
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colors2, Textures.norms2}, Textures.depth2);
        vkCmdBindVertexBuffers(currentCmdBuffer, 0, stack.longs(vertexBuf.buffer), stack.longs(0));
        vkCmdBindIndexBuffer(currentCmdBuffer, indexBuf.buffer[0], 0, VK_INDEX_TYPE_UINT32);
        pushUBO.update(0); //draw non-instanced stuff
        pushUBO.submit();
        //drawClouds();
        drawStars();
        worldType.renderCelestialBodies(stack);
        for (Matrix4f cube : cubes) {
            drawCube(cube, new Vector4f(0.95f, 0.05f, 0.95f, 1.f));
        }
        unbindImagesDrawingTo(stack, new long[]{Textures.colors2.image, Textures.norms2.image}, Textures.depth2.image);
    }
    public static void drawDDA(MemoryStack stack) {
        currentPipeline = pipelines[3];
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colors1, Textures.norms1}, Textures.depth1);
        vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
        unbindImagesDrawingTo(stack, new long[]{Textures.colors1.image, Textures.norms1.image}, Textures.depth1.image);
    }
    public static void drawSSAO(MemoryStack stack) {
        currentPipeline = pipelines[2];
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colors2}, Textures.depth2);
        vkCmdDraw(currentCmdBuffer, 3, 1, 0, 0);
        unbindImagesDrawingTo(stack, new long[]{Textures.colors2.image}, Textures.depth2.image);
    }
    public static void drawGUI(MemoryStack stack) {
        currentPipeline = pipelines[1];
        bindImagesToDrawTo(stack, currentPipeline.vkPipeline, new Texture[]{Textures.colors1}, Textures.depth1);
        Renderer.drawQuad(new Matrix4f().translate(-1.f, -1.f, 0.f).scale(2), new Vector4f(-1.f));
        GUI.draw();
        unbindImagesDrawingTo(stack, new long[]{Textures.colors1.image}, Textures.depth1.image);
    }

    public static void drawClouds() {
//        FloatBuffer modelBuffer = BufferUtils.createFloatBuffer(196 * 16);
//        FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(196 * 4);
        Random cloudRand = new Random(911);
        float brightness = Math.clamp((640 + sunPos.y()) / 640, 0.3f, 1.f);
        for (int i = 0; i < 196; i++) {
            float b = Math.max(0.25f, brightness - (cloudRand.nextFloat() / 2));
            Vector3f pos = new Vector3f(0, 0, 2000 * (cloudRand.nextFloat() + 0.05f)).rotateY((float) ((cloudRand.nextFloat() * 10) + ((Main.timeMs*0.000005f) * (3 + cloudRand.nextInt(2)))));
            drawCube(new Matrix4f().rotateY(cloudRand.nextFloat() / 10).setTranslation(pos.set(pos.x + World.halfSize, cloudRand.nextInt(200) + 420 - ((Math.abs(pos.x) + Math.abs(pos.z)) / 10), pos.z + World.halfSize)).scale(50 + cloudRand.nextInt(50), 10 + cloudRand.nextInt(20), 50 + cloudRand.nextInt(50)), new Vector4f(b, b, b, 1.f));
//            try (MemoryStack stack = MemoryStack.stackPush()) {
//                modelBuffer.put(new Matrix4f().rotateY(cloudRand.nextFloat() / 10).setTranslation(pos.set(pos.x + World.halfSize, cloudRand.nextInt(200) + 320 - ((Math.abs(pos.x) + Math.abs(pos.z)) / 10), pos.z + World.halfSize)).scale(5 + cloudRand.nextInt(5), 1 + cloudRand.nextInt(2), 5 + cloudRand.nextInt(5)).get(stack.mallocFloat(16)));
//            }
//            colorBuffer.put(b);
//            colorBuffer.put(b);
//            colorBuffer.put(b);
//            colorBuffer.put(-1);
        }
//        modelBuffer.flip();
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, modelsSSBOId);
//        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, modelsSSBOId);
//        glBufferData(GL_SHADER_STORAGE_BUFFER, modelBuffer, GL_DYNAMIC_DRAW);
//        colorBuffer.flip();
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, colorsSSBOId);
//        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, colorsSSBOId);
//        glBufferData(GL_SHADER_STORAGE_BUFFER, colorBuffer, GL_DYNAMIC_DRAW);
//        drawCubes(1024);
    }
    public static Vector3f[] starColors = new Vector3f[]{new Vector3f(0.9f, 0.95f, 1.f), new Vector3f(1, 0.95f, 0.4f), new Vector3f(0.72f, 0.05f, 0), new Vector3f(0.42f, 0.85f, 1.f), new Vector3f(0.04f, 0.3f, 1.f), new Vector3f(1, 1, 0.1f)};
    public static int starDist = (World.size*2)+200;
    public static void drawStars() {
//        FloatBuffer modelBuffer = BufferUtils.createFloatBuffer(1024*16);
//        FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(1024*4);
        Random starRand = new Random(911);
        for (int i = 0; i < 1024; i++) {
            Vector3f starPos = new Vector3f(0, starDist * 2, 0)
                    .rotateX(starRand.nextFloat() * 10)
                    .rotateY(starRand.nextFloat() * 10)
                    .rotateZ((float) (Main.timeMs*0.00001f) + starRand.nextFloat() * 10);
            starPos.set(starPos.x + (starDist / 2f), starPos.y, starPos.z + (starDist / 2f));
            float starSize = ((starRand.nextFloat()*40)+40)-Math.max(0, 150*(sunPos.y/World.size));
            if (starSize > 0.01f) {
                Matrix4f starMatrix = new Matrix4f()
                        .rotateXYZ(starRand.nextFloat(), starRand.nextFloat(), starRand.nextFloat())
                        .setTranslation(starPos)
                        .scale(starSize);
                if (starMatrix.getTranslation(new Vector3f()).y > 63-player.pos.y()) {
                    //modelBuffer.put(starMatrix.get(stack.mallocFloat(16)));
                    Vector3f color = starRand.nextFloat() < 0.64f ? new Vector3f(0.97f, 0.98f, 1.f) : starColors[starRand.nextInt(starColors.length - 1)];
//                    colorBuffer.put(color.x*12);
//                    colorBuffer.put(color.y*12);
//                    colorBuffer.put(color.z*12);
//                    colorBuffer.put(2);
                    drawCube(starMatrix, new Vector4f(color.x(), color.y(), color.z(), 1.f));
                }
            }
        }
//        modelBuffer.flip();
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, modelsSSBOId);
//        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, modelsSSBOId);
//        glBufferData(GL_SHADER_STORAGE_BUFFER, modelBuffer, GL_DYNAMIC_DRAW);
//        colorBuffer.flip();
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, colorsSSBOId);
//        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, colorsSSBOId);
//        glBufferData(GL_SHADER_STORAGE_BUFFER, colorBuffer, GL_DYNAMIC_DRAW);
//        drawCubes(1024);
    }

    public static void drawCube(Matrix4f modelMatrix, Vector4f color) {
        pushUBO.update(modelMatrix, color);
        pushUBO.submit();
        vkCmdDrawIndexed(currentCmdBuffer, Models.CUBE.indexCount, 1, Models.CUBE.indexOffset/Index.SIZE, 0, 0);
    }
    public static void drawQuad(Matrix4f modelMatrix, Vector4f color) {
        pushUBO.update(modelMatrix, color);
        pushUBO.submit();
        vkCmdDrawIndexed(currentCmdBuffer, Models.QUAD.indexCount, 1, Models.QUAD.indexOffset/Index.SIZE, Models.QUAD.vertexOffset/Vertex.SIZE, 0);
    }
    public static void drawQuadCentered(Matrix4f modelMatrix, Vector4f color) {
        pushUBO.update(modelMatrix, color);
        pushUBO.submit();
        vkCmdDrawIndexed(currentCmdBuffer, Models.QUAD_CENTERED.indexCount, 1, Models.QUAD_CENTERED.indexOffset/Index.SIZE, Models.QUAD_CENTERED.vertexOffset/Vertex.SIZE, 0);
    }

    public static boolean startCommandBuffers(MemoryStack stack) {
        int waitResult = vkWaitForFences(vkDevice, inFlightFences[frameIdx], true, Long.MAX_VALUE);
        if (waitResult != VK_SUCCESS) {throw new RuntimeException("Failed to wait for fences: "+waitResult);}

        IntBuffer imageIdxBuf = stack.mallocInt(1);
        int result = vkAcquireNextImageKHR(vkDevice, vkSwapchain, Long.MAX_VALUE, imageAvailableSemaphores[frameIdx], VK_NULL_HANDLE, imageIdxBuf);
        if (result == VK_ERROR_OUT_OF_DATE_KHR) {
            return false;
        } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {System.err.println("Failed to acquire next image!");}
        imageIdx = imageIdxBuf.get(0);
        vkResetFences(vkDevice, inFlightFences[frameIdx]);

        currentCmdBuffer = cmdBuffers[frameIdx];
        vkResetCommandBuffer(currentCmdBuffer, 0);
        CmdBufferHelper.recordCmdBuffer(stack, currentCmdBuffer);
        return true;
    }
    public static void submitCommandBuffers(MemoryStack stack) {
        vkEndCommandBuffer(currentCmdBuffer);
        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .waitSemaphoreCount(1)
                .pWaitSemaphores(stack.longs(imageAvailableSemaphores[frameIdx]))
                .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                .pCommandBuffers(stack.pointers(currentCmdBuffer.address()))
                .pSignalSemaphores(stack.longs(renderFinishedSemaphores[imageIdx]));
        vkQueueSubmit(graphicsQueue, submitInfo, inFlightFences[frameIdx]);

        VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(stack.longs(renderFinishedSemaphores[imageIdx]))
                .swapchainCount(1)
                .pSwapchains(stack.longs(vkSwapchain))
                .pImageIndices(stack.ints(imageIdx));
        int result = vkQueuePresentKHR(graphicsQueue, presentInfo);
        if (result != VK_ERROR_OUT_OF_DATE_KHR && result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
            throw new RuntimeException("Failed to queue present!");
        }
    }
    public static void unbindImagesDrawingTo(MemoryStack stack, long[] images, long depthImage) {
        vkCmdEndRendering(currentCmdBuffer);
        for (long image : images) {
            ImageHelper.transitionImageLayout(stack, currentCmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, image,
                    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                    VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT, VK_ACCESS_2_SHADER_SAMPLED_READ_BIT,
                    VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT);
        }
        ImageHelper.transitionImageLayout(stack, currentCmdBuffer, VK_IMAGE_ASPECT_DEPTH_BIT, depthImage,
                VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT, VK_ACCESS_2_SHADER_SAMPLED_READ_BIT,
                VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT, VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT);
    }
    public static void bindImagesToDrawTo(MemoryStack stack, long pipeline, Texture[] textures, Texture depthTex) {
        VkRenderingAttachmentInfo.Buffer colorAttachments = getColorAttachments(stack, currentCmdBuffer, textures);
        VkRenderingAttachmentInfo depthAttachment = getDepthAttachment(stack, currentCmdBuffer, depthTex.image, depthTex.imageView, depthTex.isLayoutUnset() ? VK_IMAGE_LAYOUT_UNDEFINED : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        VkRect2D renderAreaData = VkRect2D.calloc(stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight));
        VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_INFO)
                .renderArea(renderAreaData)
                .layerCount(1)
                .pColorAttachments(colorAttachments)
                .pDepthAttachment(depthAttachment);
        vkCmdBeginRendering(currentCmdBuffer, renderingInfo);
        vkCmdBindPipeline(currentCmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        vkCmdSetViewport(currentCmdBuffer, 0, VkViewport.calloc(1, stack).x(0).y(0).width(eWidth).height(eHeight).minDepth(0).maxDepth(1));
        vkCmdSetScissor(currentCmdBuffer, 0, VkRect2D.calloc(1, stack).offset(VkOffset2D.calloc(stack).set(0, 0)).extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight)));
    }
    public static VkRenderingAttachmentInfo.Buffer getColorAttachments(MemoryStack stack, VkCommandBuffer cmdBuffer, Texture[] textures) {
        VkRenderingAttachmentInfo.Buffer attachmentInfo = VkRenderingAttachmentInfo.calloc(textures.length, stack);
        for (int i = 0; i < textures.length; i++) {
            Texture tex = textures[i];
            int prevLayout = tex.isLayoutUnset() ? VK_IMAGE_LAYOUT_UNDEFINED : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
            ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, tex.image,
                    prevLayout, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                    prevLayout == VK_IMAGE_LAYOUT_UNDEFINED ? VK_ACCESS_2_NONE : VK_ACCESS_2_SHADER_SAMPLED_READ_BIT, VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                    prevLayout == VK_IMAGE_LAYOUT_UNDEFINED ? VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT : VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT, VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);
            attachmentInfo.get(i)
                    .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                    .imageView(tex.imageView)
                    .imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            attachmentInfo.clearValue().color().float32(0, 0.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 0.0f);
        }
        return attachmentInfo;
    }
    public static void unbindPresentImage(MemoryStack stack) {
        vkCmdEndRendering(currentCmdBuffer);
        ImageHelper.transitionImageLayout(stack, currentCmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, Swapchain.images[imageIdx],
                VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT, VK_ACCESS_2_NONE,
                VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_2_NONE);
    }
    public static void bindPresentImage(MemoryStack stack) {
        VkRenderingAttachmentInfo.Buffer colorAttachment = getPresentColorAttachment(stack, currentCmdBuffer, images[imageIdx], imageViews[imageIdx], firstImages ? VK_IMAGE_LAYOUT_UNDEFINED : VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
        VkRenderingAttachmentInfo depthAttachment = getDepthAttachment(stack, currentCmdBuffer, depthImage, depthImageView, VK_IMAGE_LAYOUT_UNDEFINED);
        VkRect2D renderAreaData = VkRect2D.calloc(stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight));
        VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_INFO)
                .renderArea(renderAreaData)
                .layerCount(1)
                .pColorAttachments(colorAttachment)
                .pDepthAttachment(depthAttachment);
        vkCmdBeginRendering(currentCmdBuffer, renderingInfo);
        currentPipeline = pipelines[0];
        vkCmdBindPipeline(currentCmdBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, currentPipeline.vkPipeline);
        vkCmdSetViewport(currentCmdBuffer, 0, VkViewport.calloc(1, stack).x(0).y(0).width(eWidth).height(eHeight).minDepth(0).maxDepth(1));
        vkCmdSetScissor(currentCmdBuffer, 0, VkRect2D.calloc(1, stack).offset(VkOffset2D.calloc(stack).set(0, 0)).extent(VkExtent2D.calloc(stack).width(eWidth).height(eHeight)));
    }

    public static VkRenderingAttachmentInfo.Buffer getPresentColorAttachment(MemoryStack stack, VkCommandBuffer cmdBuffer, long image, long imageView, int prevLayout) {
        ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_COLOR_BIT, image,
                prevLayout, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK_ACCESS_2_NONE, VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT);
        VkRenderingAttachmentInfo.Buffer attachmentInfo = VkRenderingAttachmentInfo.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                .imageView(imageView)
                .imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        attachmentInfo.clearValue().color().float32(0, 0.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 0.0f);
        return attachmentInfo;
    }
    public static VkRenderingAttachmentInfo getDepthAttachment(MemoryStack stack, VkCommandBuffer cmdBuffer, long image, long imageView, int prevLayout) {
        if (depthImage == image) {
            ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_DEPTH_BIT, image,
                    VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL,
                    VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT, VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                    VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT, VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT);
        } else {
            ImageHelper.transitionImageLayout(stack, cmdBuffer, VK_IMAGE_ASPECT_DEPTH_BIT, image,
                    prevLayout, VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL,
                    VK_ACCESS_2_NONE, VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                    VK_PIPELINE_STAGE_2_NONE, VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT);
        }
        VkRenderingAttachmentInfo attachmentInfo = VkRenderingAttachmentInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                .imageView(imageView)
                .imageLayout(VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        attachmentInfo.clearValue().depthStencil().depth(0.f);
        return attachmentInfo;
    }

    public static int lodSSBOSize = World.lods.length*8;
    public static int gigabyte = 1000000000;
    public static int voxelSSBOSize = gigabyte/2;
    public static int chunkArrSize = sizeChunks*sizeChunks*heightChunks;
    public static int chunkByteSize = 4*4;
    public static int chunkSSBOSize = chunkArrSize*chunkByteSize;
    public static PointerBuffer blocks = BufferUtils.createPointerBuffer(1);
    public static long[] chunkBlockAllocs;
    public static void prepareTestScene() {
        worldType.generate();
        VmaVirtualBlockCreateInfo blockCreateInfo = VmaVirtualBlockCreateInfo.create();
        blockCreateInfo.size(voxelSSBOSize);
        vmaCreateVirtualBlock(blockCreateInfo, blocks);
        long chunkPtr = Graphics.chunkSSBO.stagingBuffer.pointer.get(0);
        long voxelPtr = Graphics.voxelSSBO.stagingBuffer.pointer.get(0);
        chunkBlockAllocs = new long[chunkArrSize];

        outerLoop:
        for (int chunkX = 0; chunkX < sizeChunks; chunkX++) {
            for (int chunkZ = 0; chunkZ < sizeChunks; chunkZ++) {
                for (int chunkY = 0; chunkY < heightChunks; chunkY++) {
                    VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
                    int packedChunkPos = World.packChunkPos(chunkX, chunkY, chunkZ);
                    int paletteSize = chunks[packedChunkPos].getBlockPaletteSize();
                    int bitsPerValue = chunks[packedChunkPos].bitsPerBlock();
                    int valueMask = chunks[packedChunkPos].blockValueMask();
                    int[] compressedBlocks = chunks[packedChunkPos].getBlockData();
                    if (compressedBlocks == null) {
                        allocCreateInfo.size((paletteSize) * 4L);
                    } else {
                        allocCreateInfo.size((paletteSize + compressedBlocks.length) * 4L);
                    }

                    PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
                    LongBuffer offset = BufferUtils.createLongBuffer(1);
                    long res = vmaVirtualAllocate(blocks.get(0), allocCreateInfo, alloc, offset);
                    if (res == VK_SUCCESS) {
                        chunkBlockAllocs[packedChunkPos] = alloc.get();
                        int pointer = (int) offset.get(0);
                        MemoryUtil.memCopy(MemoryUtil.memAddress(MemoryUtil.memAlloc(chunkByteSize).asIntBuffer()
                                        .put(new int[]{pointer/4, paletteSize, bitsPerValue, valueMask}).rewind()),
                                chunkPtr+((long) packedChunkPos * chunkByteSize), chunkByteSize);
                        int[] palette = chunks[packedChunkPos].getBlockPalette();
                        int paletteSizeBytes = paletteSize*4;
                        MemoryUtil.memCopy(MemoryUtil.memAddress(MemoryUtil.memAlloc(paletteSizeBytes).asIntBuffer().put(palette).rewind()), voxelPtr+pointer, paletteSizeBytes);
                        if (compressedBlocks != null) {
                            MemoryUtil.memCopy(MemoryUtil.memAddress(MemoryUtil.memAlloc(compressedBlocks.length*4).asIntBuffer().put(compressedBlocks).rewind()), voxelPtr+pointer+paletteSizeBytes, compressedBlocks.length*4L);
                        }
                    } else {
                        System.out.print("blocksSSBO ran out of space! \n");
                        Main.isClosing = true;
                        break outerLoop;
                    }
                }
            }
        }

        long lodPtr = Graphics.lodSSBO.stagingBuffer.pointer.get(0);
        MemoryUtil.memByteBuffer(lodPtr, lodSSBOSize).asLongBuffer().put(World.lods).rewind();

        VkBufferCopy.Buffer chunkBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(chunkSSBOSize);
        vkCmdCopyBuffer(currentCmdBuffer, Graphics.chunkSSBO.stagingBuffer.buffer[0], Graphics.chunkSSBO.buffer.buffer[0], chunkBufferCopy);
        VkBufferCopy.Buffer voxelBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(voxelSSBOSize);
        vkCmdCopyBuffer(currentCmdBuffer, Graphics.voxelSSBO.stagingBuffer.buffer[0], Graphics.voxelSSBO.buffer.buffer[0], voxelBufferCopy);
        VkBufferCopy.Buffer lodBufferCopy = VkBufferCopy.calloc(1).srcOffset(0).dstOffset(0).size(lodSSBOSize);
        vkCmdCopyBuffer(currentCmdBuffer, Graphics.lodSSBO.stagingBuffer.buffer[0], Graphics.lodSSBO.buffer.buffer[0], lodBufferCopy);
        VkBufferMemoryBarrier.Buffer barrierBuf = VkBufferMemoryBarrier.calloc(3);
        barrierBuf.get(0)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(Graphics.chunkSSBO.buffer.buffer[0])
                .offset(0).size(chunkSSBOSize);
        barrierBuf.get(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(Graphics.voxelSSBO.buffer.buffer[0])
                .offset(0).size(voxelSSBOSize);
        barrierBuf.get(2)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(Graphics.lodSSBO.buffer.buffer[0])
                .offset(0).size(lodSSBOSize);
        vkCmdPipelineBarrier(currentCmdBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, barrierBuf, null);
    }
}
