package org.conspiracraft.renderer.assets;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.conspiracraft.renderer.Renderer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Models {
    public static List<Model> models = new ArrayList<>(List.of());

    public static Model SCREEN_TRIANGLE;
    public static Model SCREEN_TRIANGLE_HALF;
    public static Model QUAD;
    public static Model QUAD_UNNORMALIZED;
    public static Model PLANE_DB;
    public static Model CUBE;
    public static Model TORUS;
    public static Model HUMAN;

    public static void loadModels() {
        //long modelsInitStarted = System.currentTimeMillis();
        SCREEN_TRIANGLE = new Model(new float[]{-1, -1, 0, 3, -1, 0, -1, 3, 0});
        createVao(SCREEN_TRIANGLE);
        SCREEN_TRIANGLE_HALF = new Model(new float[]{0, -3, 0, 0, 0, 0, -3, 0, 0});
        createVao(SCREEN_TRIANGLE_HALF);
        QUAD = loadObj("generic/model/quad");
        QUAD_UNNORMALIZED = loadObj("generic/model/quad_unnormalized");
        PLANE_DB = loadObj("generic/model/plane_db");
        CUBE = loadObj("generic/model/cube");
        TORUS = loadObj("generic/model/torus");
        //System.out.print("Took "+String.format("%.2f", (System.currentTimeMillis()-modelsInitStarted)/1000.f)+"s to load models.\n");
    }

    public static FloatArrayList verts = new FloatArrayList();
    public static FloatArrayList normals = new FloatArrayList();
    public static FloatArrayList vertPositions = new FloatArrayList();
    public static FloatArrayList vertNormals = new FloatArrayList();
    public static void clearArrays() {
        verts.clear();
        normals.clear();
        vertPositions.clear();
        vertNormals.clear();
    }

    public static void createVao(Model model) {
        int vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
        glBufferData(GL_ARRAY_BUFFER, model.positions, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        if (model.normals != null) {
            glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
            glBufferData(GL_ARRAY_BUFFER, model.normals, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        }
        model.vaoId = vaoId;
    }

    public static Model loadObj(String name) {
        clearArrays();
        BufferedReader reader = new BufferedReader(new InputStreamReader(Renderer.class.getClassLoader().getResourceAsStream("assets/base/"+name+".obj")));
        reader.lines().forEach((String line) -> {
            String[] parts = line.split("\\s+");
            if (parts[0].equals("v")) {
                verts.addLast(Float.parseFloat(parts[1]));
                verts.addLast(Float.parseFloat(parts[2]));
                verts.addLast(Float.parseFloat(parts[3]));
            } else if (parts[0].equals("vn")) {
                normals.addLast(Float.parseFloat(parts[1]));
                normals.addLast(Float.parseFloat(parts[2]));
                normals.addLast(Float.parseFloat(parts[3]));
            } else if (parts[0].equals("f")) {
                createVertex(parts[1].split("//"));
                createVertex(parts[2].split("//"));
                createVertex(parts[3].split("//"));
            }
        });
        Model model = new Model(vertPositions.toFloatArray(), vertNormals.toFloatArray());
        createVao(model);
        models.addLast(model);
        return model;
    }
    public static void createVertex(String[] vertex) {
        int vertId = (Integer.parseInt(vertex[0])-1)*3;
        vertPositions.addLast(verts.get(vertId));
        vertPositions.addLast(verts.get(1+vertId));
        vertPositions.addLast(verts.get(2+vertId));
        int normId = (Integer.parseInt(vertex[1])-1)*3;
        vertNormals.addLast(normals.get(normId));
        vertNormals.addLast(normals.get(1+normId));
        vertNormals.addLast(normals.get(2+normId));
    }
}
