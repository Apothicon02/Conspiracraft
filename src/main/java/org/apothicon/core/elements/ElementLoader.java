package org.apothicon.core.elements;

import org.apothicon.core.utilities.Utils;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL40;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class ElementLoader {
    private List<Integer> vaos = new ArrayList<>();
    private List<Integer> vbos = new ArrayList<>();
    private List<Integer> textures = new ArrayList<>();

    public Model loadOBJModel(String fileName) {
        List<String> lines = Utils.readAllLines(fileName);

        List<Vector3f> vertices = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> textures = new ArrayList<>();
        List<Vector3i> faces = new ArrayList<>();

        for (String line : lines) {
            String[] tokens = line.split("\\s+");
            switch (tokens[0]) {
                case "vt":
                    Vector2f texturesVec = new Vector2f(
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2])
                    );
                    textures.add(texturesVec);
                    break;
                case "v":
                    Vector3f verticesVec = new Vector3f(
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3])
                    );
                    vertices.add(verticesVec);
                    break;
                case "vn":
                    Vector3f normalsVec = new Vector3f(
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3])
                    );
                    normals.add(normalsVec);
                    break;
                case "f":
                    for (int i = 1; i < tokens.length; i++) {
                        processFace(tokens[i], faces);
                    }
                    break;
                default:
                    break;
            }
        }
        List<Integer> indices = new ArrayList<>();
        float[] verticesArray = new float[vertices.size() * 3];
        int i = 0;
        for (Vector3f pos : vertices) {
            verticesArray[i*3] = pos.x;
            verticesArray[i*3+1] = pos.y;
            verticesArray[i*3+2] = pos.z;
            i++;
        }
        float[] textureCoordArray = new float[vertices.size() * 2];
        float[] normalArray = new float[vertices.size() * 3];
        for (Vector3i face : faces) {
            processVertex(face.x, face.y, face.z, textures, normals, indices, textureCoordArray, normalArray);
        }

        int[] indicesArray = indices.stream().mapToInt((Integer v) -> v).toArray();

        return loadModel(verticesArray, textureCoordArray, indicesArray);
    }

    private static void processVertex(int pos, int textureCoord, int normal, List<Vector2f> textureCoordList, List<Vector3f> normalList, List<Integer> indicesList, float[] textureCoordArray, float[] normalArray) {
        indicesList.add(pos);

        if (textureCoord >= 0) {
            Vector2f textureCoordVec = textureCoordList.get(textureCoord);
            textureCoordArray[pos * 2] = textureCoordVec.x;
            textureCoordArray[pos * 2 + 1] = 1 - textureCoordVec.y;
        }

        if (normal >= 0) {
            Vector3f normalVec = normalList.get(normal);
            normalArray[pos * 3] = normalVec.x;
            normalArray[pos * 3 + 1] = normalVec.y;
            normalArray[pos * 3 + 2] = normalVec.z;
        }
    }

    private static void processFace(String token, List<Vector3i> faces) {
        String[] lineToken = token.split("/");
        int length = lineToken.length;
        int coords = -1, normal = -1;
        int pos = (int) (Float.parseFloat(lineToken[0]) -1);
        if (length > 1) {
            String textureCoord = lineToken[1];
            coords = !textureCoord.isEmpty() ? Integer.parseInt(textureCoord) - 1 : -1;
            if (length > 2) {
                normal = Integer.parseInt(lineToken[2]) -1;
            }
        }
        Vector3i facesVec = new Vector3i(pos, coords, normal);
        faces.add(facesVec);
    }

    public Model loadModel(float[] vertices, float[] texCoords, int[] indices) {
        int id = createVAO();
        storeIndicesBuffer(indices);
        storeDataInAttributeList(0, 3, vertices);
        storeDataInAttributeList(1, 2, texCoords);
        unbindVAO();
        return new Model(id, indices.length);
    }

    public int loadTexture(String fileName) throws Exception {
        int width, height;
        ByteBuffer buffer;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);

            buffer = STBImage.stbi_load(fileName, w, h, c, 4);

            if (buffer == null) {
                throw new Exception("Image File " + fileName + " not loaded " + STBImage.stbi_failure_reason());
            }

            width = w.get();
            height = h.get();
        }

        int id = GL40.glGenTextures();
        textures.add(id);
        GL40.glBindTexture(GL40.GL_TEXTURE_2D, id);
        GL40.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL40.glTexImage2D(GL40.GL_TEXTURE_2D, 0, GL40.GL_RGBA, width, height, 0, GL40.GL_RGBA, GL40.GL_UNSIGNED_BYTE, buffer);
        GL40.glGenerateMipmap(GL40.GL_TEXTURE_2D);
        STBImage.stbi_image_free(buffer);
        return id;
    }

    private int createVAO() {
        int id = GL40.glGenVertexArrays();
        vaos.add(id);
        GL40.glBindVertexArray(id);
        return id;
    }

    private  void storeIndicesBuffer(int[] indices) {
        int vbo = GL40.glGenBuffers();
        vbos.add(vbo);
        GL40.glBindBuffer(GL40.GL_ELEMENT_ARRAY_BUFFER, vbo);
        IntBuffer buffer = Utils.storeDataInIntBuffer(indices);
        GL40.glBufferData(GL40.GL_ELEMENT_ARRAY_BUFFER, buffer, GL40.GL_STATIC_DRAW);
    }

    private void storeDataInAttributeList(int attributeNum, int vertexCount, float[] data) {
        int vbo = GL40.glGenBuffers();
        vbos.add(vbo);
        GL40.glBindBuffer(GL40.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = Utils.storeDataInFloatBuffer(data);
        GL40.glBufferData(GL40.GL_ARRAY_BUFFER, buffer, GL40.GL_STATIC_DRAW);
        GL40.glVertexAttribPointer(attributeNum, vertexCount, GL40.GL_FLOAT, false, 0, 0);
        GL40.glBindBuffer(GL40.GL_ARRAY_BUFFER, 0);
    }

    private void unbindVAO() {
        GL40.glBindVertexArray(0);
    }

    public void cleanup() {
        for (int vao : vaos) {
            GL40.glDeleteVertexArrays(vao);
        }
        for (int vbo : vbos) {
            GL40.glDeleteBuffers(vbo);
        }
        for (int texture : textures) {
            GL40.glDeleteTextures(texture);
        }
    }
}
