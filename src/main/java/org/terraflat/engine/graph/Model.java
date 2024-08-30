package org.terraflat.engine.graph;

import org.terraflat.engine.scene.Entity;

import java.util.*;

public class Model {

    private final String id;
    private Map<String, Entity> entitiesMap;
    private List<Material> materialList;
    boolean cullNegX = true;
    boolean cullPosX = true;
    boolean cullNegY = true;
    boolean cullPosY = true;
    boolean cullNegZ = true;
    boolean cullPosZ = true;

    public Model(String id, List<Material> materialList) {
        this.id = id;
        entitiesMap = new HashMap<>();
        this.materialList = materialList;
    }

    public void cleanup() {
        materialList.forEach(Material::cleanup);
    }

    public Map<String, Entity> getEntitiesMap() {
        return entitiesMap;
    }

    public String getId() {
        return id;
    }

    public List<Material> getMaterialList() {
        return materialList;
    }
}