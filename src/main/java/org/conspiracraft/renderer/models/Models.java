package org.conspiracraft.renderer.models;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.conspiracraft.renderer.Renderer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Models {
    public static List<Model> models = new ArrayList<>(List.of());

    public static Model SCREEN_TRIANGLE;
    public static Model CUBE;

    public static void loadModels(long mappedPtr) {
//        SCREEN_TRIANGLE = new Model(new float[]{-1, -1, 0, 3, -1, 0, -1, 3, 0});
//        createVao(SCREEN_TRIANGLE);
        CUBE = loadObj(mappedPtr, "generic/model/cube");
    }

    public static FloatArrayList verts = new FloatArrayList();
    public static FloatArrayList normals = new FloatArrayList();
    public static FloatArrayList vertPositions = new FloatArrayList();
    public static FloatArrayList vertNormals = new FloatArrayList();
    public static FloatArrayList uvs = new FloatArrayList();
    public static FloatArrayList vertUVs = new FloatArrayList();
    public static void clearArrays() {
        verts.clear();
        normals.clear();
        vertPositions.clear();
        vertNormals.clear();
        uvs.clear();
        vertUVs.clear();
    }

    public static Model loadObj(long mappedPtr, String name) {
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
            } else if (parts[0].equals("vt")) {
                uvs.addLast(Float.parseFloat(parts[1]));
                uvs.addLast(Float.parseFloat(parts[2]));
            } else if (parts[0].equals("f")) {
                createVertex(parts[1].split("/"));
                createVertex(parts[2].split("/"));
                createVertex(parts[3].split("/"));
            }
        });
        Model model = new Model(mappedPtr, vertPositions.toFloatArray(), vertNormals.toFloatArray(), vertUVs.toFloatArray());
        models.addLast(model);
        return model;
    }
    public static void createVertex(String[] vertex) {
        int vertId = (Integer.parseInt(vertex[0])-1)*3;
        vertPositions.addLast(verts.get(vertId));
        vertPositions.addLast(verts.get(1+vertId));
        vertPositions.addLast(verts.get(2+vertId));
        int uvId = (Integer.parseInt(vertex[1]) - 1) * 2;
        vertUVs.addLast(uvs.get(uvId));
        vertUVs.addLast(uvs.get(1+uvId));
        int normId = (Integer.parseInt(vertex[2])-1)*3;
        vertNormals.addLast(normals.get(normId));
        vertNormals.addLast(normals.get(1+normId));
        vertNormals.addLast(normals.get(2+normId));
    }
}