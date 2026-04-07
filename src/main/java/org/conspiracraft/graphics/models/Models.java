package org.conspiracraft.graphics.models;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.conspiracraft.graphics.Renderer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Models {
    public static List<Model> models = new ArrayList<>(List.of());

    public static Model CUBE;

    public static void loadModels(long vertexPtr, long indexPtr) {
        CUBE = loadObj(vertexPtr, indexPtr, "generic/model/cube");
    }

    public static IntArrayList indices = new IntArrayList();
    public static FloatArrayList verts = new FloatArrayList();
    public static void clearArrays() {
        indices.clear();
        verts.clear();
    }

    public static Model loadObj(long vertexPtr, long indexPtr, String name) {
        clearArrays();
        BufferedReader reader = new BufferedReader(new InputStreamReader(Renderer.class.getClassLoader().getResourceAsStream("assets/base/"+name+".obj")));
        reader.lines().forEach((String line) -> {
            String[] parts = line.split("\\s+");
            if (parts[0].equals("v")) {
                verts.addLast(Float.parseFloat(parts[1]));
                verts.addLast(Float.parseFloat(parts[2]));
                verts.addLast(Float.parseFloat(parts[3]));
            } else if (parts[0].equals("f")) {
                indices.addLast(Integer.parseInt(parts[1])-1);
                indices.addLast(Integer.parseInt(parts[2])-1);
                indices.addLast(Integer.parseInt(parts[3])-1);
            }
        });
        Model model = new Model(vertexPtr, indexPtr, verts.toFloatArray(), indices.toIntArray());
        models.addLast(model);
        return model;
    }
}