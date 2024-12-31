package org.conspiracraft.game.rendering;

import org.conspiracraft.Main;
import org.conspiracraft.game.Noise;
import org.conspiracraft.game.blocks.Block;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.LightBlockType;
import org.conspiracraft.game.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4i;
import org.lwjgl.BufferUtils;
import org.conspiracraft.engine.*;
import org.conspiracraft.engine.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;
import static org.conspiracraft.game.world.World.*;
import static org.lwjgl.util.vma.Vma.vmaCreateVirtualBlock;
import static org.lwjgl.util.vma.Vma.vmaVirtualAllocate;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class Renderer {
    public static ShaderProgram scene;
    public static Projection projection;
    public static int sceneVaoId;
    public static int atlasSSBOId;
    public static int chunksSSBOId;
    public static int region1SSBOId;
    public static int chunksLightSSBOId;
    public static int region1LightingSSBOId;
    public static int region1CornersSSBOId;
    public static int coherentNoiseId;
    public static int whiteNoiseId;
    public static int resUniform;
    public static int camUniform;
    public static int renderDistanceUniform;
    public static int timeOfDayUniform;
    public static int timeUniform;
    public static int selectedUniform;
    public static int uiUniform;
    public static int sunUniform;
    public static int raytraceUniform;
    public static int projectionUniform;
    public static int viewUniform;
    public static int modelUniform;
    public static int renderDistanceMul = 4;
    public static float timeOfDay = 0.5f;
    public static double time = 0.5d;
    public static boolean atlasChanged = true;
    public static boolean worldChanged = false;
    public static boolean[] collisionData = new boolean[9984*9984+9984];
    public static boolean showUI = true;
    public static PointerBuffer block = BufferUtils.createPointerBuffer(1);
    public static PointerBuffer light = BufferUtils.createPointerBuffer(1);
    public static int[] chunkPointers =  new int[(((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks];
    public static int[] chunkLightPointers =  new int[(((sizeChunks*sizeChunks)+sizeChunks)*heightChunks)+heightChunks];

    public static void init(Window window) throws Exception {
        projection = new Projection(window.getWidth(), window.getHeight());
        sceneVaoId = glGenVertexArrays();
        glBindVertexArray(sceneVaoId);
        FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(117);
        verticesBuffer.put(new float[]{
                -1, -1, 0, 3, -1, 0, -1, 3, 0,

                -20, -20, -20, -20, 20, -20, 20, -20, -20,
                20, 20, -20, -20, 20, -20, 20, -20, -20,

                -20, -20, 20, -20, 20, 20, 20, -20, 20,
                20, 20, 20, -20, 20, 20, 20, -20, 20,

                -20, 20, -20, -20, 20, 20, 20, 20, -20,
                20, 20, 20, -20, 20, 20, 20, 20, -20,

                -20, -20, -20, -20, -20, 20, 20, -20, -20,
                20, -20, 20, -20, -20, 20, 20, -20, -20,

                20, -20, -20, 20, -20, 20, 20, 20, -20,
                -20, 20, 20, -20, -20, 20, -20, 20, -20,

                -20, -20, -20, -20, -20, 20, -20, 20, -20,
                20, 20, 20, 20, -20, 20, 20, 20, -20,
        }).flip();
        glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        scene = new ShaderProgram();
        scene.createVertexShader(Utils.readFile("assets/base/shaders/scene.vert"));
        scene.createFragmentShader(Utils.readFile("assets/base/shaders/scene.frag"));
        scene.link();

        coherentNoiseId = glGenTextures();
        whiteNoiseId = glGenTextures();
        atlasSSBOId = glGenBuffers();
        chunksSSBOId = glGenBuffers();
        region1SSBOId = glGenBuffers();
        chunksLightSSBOId = glGenBuffers();
        region1LightingSSBOId = glGenBuffers();
        region1CornersSSBOId = glGenBuffers();

        glBindTexture(GL_TEXTURE_2D, coherentNoiseId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 2048, 2048, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(Noise.COHERERENT_NOISE));

        glBindTexture(GL_TEXTURE_2D, whiteNoiseId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1024, 1024, 0, GL_RGBA, GL_UNSIGNED_BYTE, Utils.imageToBuffer(Noise.WHITE_NOISE));

        resUniform = glGetUniformLocation(scene.programId, "res");
        camUniform = glGetUniformLocation(scene.programId, "cam");
        renderDistanceUniform = glGetUniformLocation(scene.programId, "renderDistance");
        timeOfDayUniform = glGetUniformLocation(scene.programId, "timeOfDay");
        timeUniform = glGetUniformLocation(scene.programId, "time");
        selectedUniform = glGetUniformLocation(scene.programId, "selected");
        uiUniform = glGetUniformLocation(scene.programId, "ui");
        sunUniform = glGetUniformLocation(scene.programId, "sun");
        raytraceUniform = glGetUniformLocation(scene.programId, "raytrace");
        projectionUniform = glGetUniformLocation(scene.programId, "projection");
        viewUniform = glGetUniformLocation(scene.programId, "view");
        modelUniform = glGetUniformLocation(scene.programId, "model");

        VmaVirtualBlockCreateInfo blockCreateInfo = VmaVirtualBlockCreateInfo.create();
        blockCreateInfo.size(Integer.MAX_VALUE);
        long result = vmaCreateVirtualBlock(blockCreateInfo, block);
        if (result != VK_SUCCESS) {
            int nothing = 0;
        }
        result = vmaCreateVirtualBlock(blockCreateInfo, light);
        if (result != VK_SUCCESS) {
            int nothing = 0;
        }
    }

    public static void render(Window window) throws IOException {
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        scene.bind();

        glUniform2f(resUniform, window.getWidth(), window.getHeight());
        Matrix4f camMatrix = Main.player.getCameraMatrix();
        glUniformMatrix4fv(camUniform, true, new float[]{
                camMatrix.m00(), camMatrix.m10(), camMatrix.m20(), camMatrix.m30(),
                camMatrix.m01(), camMatrix.m11(), camMatrix.m21(), camMatrix.m31(),
                camMatrix.m02(), camMatrix.m12(), camMatrix.m22(), camMatrix.m32(),
                camMatrix.m03(), camMatrix.m13(), camMatrix.m23(), camMatrix.m33()});
        glUniform1i(renderDistanceUniform, 150+(40*renderDistanceMul));
        glUniform1f(timeOfDayUniform, timeOfDay);
        glUniform1d(timeUniform, time);
        Vector3f selected = Main.raycast(new Matrix4f(Main.player.getCameraMatrix()), true, 100);
        if (selected == null) {
            selected = new Vector3f(-1000, -1000, -1000);
        }
        glUniform3i(selectedUniform, (int) selected.x, (int) selected.y, (int) selected.z);
        glUniform1i(uiUniform, showUI ? 1 : 0);
        float halfSize = size/2f;
        Vector3f sunPos = new Vector3f(size/4f, 0, size/4f);
        sunPos.rotateY((float) time);
        sunPos = new Vector3f(sunPos.x+halfSize, height, sunPos.z+halfSize);
        glUniform3f(sunUniform, sunPos.x, sunPos.y, sunPos.z);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(projectionUniform, false, projection.getProjMatrix().get(stack.mallocFloat(16)));
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(viewUniform, false, new Matrix4f(camMatrix).setTranslation(camMatrix.m30()*-1, camMatrix.m31()*-1, camMatrix.m32()).get(stack.mallocFloat(16)));
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(modelUniform, false, new Matrix4f().translate(sunPos.x, sunPos.y, sunPos.z*-1).get(stack.mallocFloat(16)));
        }

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, coherentNoiseId);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, whiteNoiseId);
        if (atlasChanged) {
            atlasChanged = false;
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, atlasSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, atlasSSBOId);

            BufferedImage atlasImage = ImageIO.read(Renderer.class.getClassLoader().getResourceAsStream("assets/base/textures/atlas.png"));
            int size = 9984*9984+9984;
            int[] atlasData = new int[size];
            for (int x = 0; x < 144; x++) {
                for (int y = 0; y < 256; y++) {
                    atlasData[(9984*x)+y] = Utils.colorToInt(new Color(atlasImage.getRGB(x, y), true));
                    collisionData[(9984*x)+y] = new Color(atlasImage.getRGB(x, y), true).getAlpha() != 0;
                }
            }

            IntBuffer atlasBuffer = BufferUtils.createIntBuffer(size).put(atlasData).flip();
            glBufferData(GL_SHADER_STORAGE_BUFFER, atlasBuffer, GL_STATIC_DRAW);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }
        if (worldChanged) {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1SSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, region1SSBOId);
            glBufferData(GL_SHADER_STORAGE_BUFFER, (size*size*height)*4, GL_DYNAMIC_DRAW);

            for (int chunkX = 0; chunkX < sizeChunks; chunkX++) {
                for (int chunkZ = 0; chunkZ < sizeChunks; chunkZ++) {
                    for (int chunkY = 0; chunkY < heightChunks; chunkY++) {
                        VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
                        allocCreateInfo.size(16384);

                        PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
                        LongBuffer offset = BufferUtils.createLongBuffer(1);
                        long res = vmaVirtualAllocate(block.get(0), allocCreateInfo, alloc, offset);
                        if (res == VK_SUCCESS) {
                            int condensedChunkPos = World.condenseChunkPos(chunkX, chunkY, chunkZ);
                            int pointer = (int) offset.get(0);
                            chunkPointers[condensedChunkPos] = pointer/4;
                            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, region1Chunks[condensedChunkPos].getAllBlocks());
                        } else {
                            // Allocation failed - no space for it could be found. Handle this error!
                        }
                    }
                }
            }

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        } else {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1SSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, region1SSBOId);
            for (int i = 0; i < Math.min(Math.min(Engine.fps, 100), blockQueue.size()); i++) {
                Vector4i blockData = blockQueue.getFirst();
                blockQueue.removeFirst();
                Vector3i pos = new Vector3i(blockData.x, blockData.y, blockData.z);
                Block oldBlock = getBlock(pos);
                byte r = 0;
                byte g = 0;
                byte b = 0;
                if (BlockTypes.blockTypeMap.get(Utils.unpackInt(blockData.w()).x) instanceof LightBlockType lType) {
                    r = lType.r;
                    g = lType.g;
                    b = lType.b;
                    updateLight(pos);
                }
                Vector3i chunkPos = new Vector3i(pos.x/chunkSize, pos.y/chunkSize, pos.z/chunkSize);
                int condensedChunkPos = World.condenseChunkPos(chunkPos);
                int localPos = condenseLocalPos(pos.x-(chunkPos.x*chunkSize), pos.y-(chunkPos.y*chunkSize), pos.z-(chunkPos.z*chunkSize));
                region1Chunks[condensedChunkPos].setBlock(localPos, new Block(blockData.w, r, g, b, (byte) 0), pos);
                updateHeightmap(blockData.x, blockData.z, true);
                recalculateLight(pos, oldBlock.r(), oldBlock.g(), oldBlock.b(), oldBlock.s());
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, (chunkPointers[condensedChunkPos]+localPos)*4L, new int[]{blockData.w});
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunksSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, chunksSSBOId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkPointers, GL_DYNAMIC_DRAW); //change to only upload changes for better fps
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        if (worldChanged) {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1LightingSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, region1LightingSSBOId);
            glBufferData(GL_SHADER_STORAGE_BUFFER, (size*size*height)*4, GL_DYNAMIC_DRAW);

            for (int chunkX = 0; chunkX < sizeChunks; chunkX++) {
                for (int chunkZ = 0; chunkZ < sizeChunks; chunkZ++) {
                    for (int chunkY = 0; chunkY < heightChunks; chunkY++) {
                        VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.create();
                        allocCreateInfo.size(16384);

                        PointerBuffer alloc = BufferUtils.createPointerBuffer(1);
                        LongBuffer offset = BufferUtils.createLongBuffer(1);
                        long res = vmaVirtualAllocate(light.get(0), allocCreateInfo, alloc, offset);
                        if (res == VK_SUCCESS) {
                            int condensedChunkPos = World.condenseChunkPos(chunkX, chunkY, chunkZ);
                            int pointer = (int) offset.get(0);
                            chunkLightPointers[condensedChunkPos] = pointer/4;
                            glBufferSubData(GL_SHADER_STORAGE_BUFFER, pointer, region1Chunks[condensedChunkPos].getAllLights());
                        } else {
                            // Allocation failed - no space for it could be found. Handle this error!
                        }
                    }
                }
            }

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        } else {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1LightingSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, region1LightingSSBOId);
            for (int i = 0; i < 400; i++) {
                if (!lightQueue.isEmpty()) {
                    Vector3i lightPos = lightQueue.getFirst();
                    lightQueue.removeFirst();
                    Vector3i chunkPos = new Vector3i(lightPos.x/chunkSize, lightPos.y/chunkSize, lightPos.z/chunkSize);
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, (chunkLightPointers[World.condenseChunkPos(chunkPos)]+ condenseLocalPos(lightPos.x-(chunkPos.x*chunkSize), lightPos.y-(chunkPos.y*chunkSize), lightPos.z-(chunkPos.z*chunkSize)))*4L, new int[]{updateLight(lightPos)});
                } else {
                    break;
                }
            }
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, chunksLightSSBOId);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, chunksLightSSBOId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, chunkLightPointers, GL_DYNAMIC_DRAW); //change to only upload changes for better fps
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        if (worldChanged) {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1CornersSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, region1CornersSSBOId);
            glBufferData(GL_SHADER_STORAGE_BUFFER, size*size*height, GL_DYNAMIC_DRAW);

            ByteBuffer corners = ByteBuffer.allocate(height);
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    for (int y = 0; y < height; y++) {
                        corners.put(y, getCorners(x, y, z));
                    }
                    corners.flip();
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, ((x*size)+z)*height, corners);
                    corners.flip();
                    corners.clear();
                }
            }

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        } else {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, region1CornersSSBOId);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, region1CornersSSBOId);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }

        glUniform1i(raytraceUniform, 1);

        glBindVertexArray(sceneVaoId);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glUniform1i(raytraceUniform, 0);
        glDrawArrays(GL_TRIANGLES, 3, 36);
        glDisableVertexAttribArray(0);

        scene.unbind();
        worldChanged = false;
    }

    public void cleanup() {
        glDeleteVertexArrays(sceneVaoId);
        scene.cleanup();
    }
}