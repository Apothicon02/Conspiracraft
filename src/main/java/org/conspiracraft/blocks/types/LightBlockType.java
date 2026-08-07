package org.conspiracraft.blocks.types;

import org.conspiracraft.blocks.Material;

import java.util.Map;

public class LightBlockType extends BlockType {
    public LightBlockProperties lightBlockProperties() {
        return (LightBlockProperties) blockProperties;
    }

    public LightBlockType(int id, String name, Map<Integer, Material> materials, LightBlockProperties blockProperties) {super(id, name, materials, blockProperties);}
    public LightBlockType(int id, String name, LightBlockProperties blockProperties) {super(id, name, blockProperties);}
}
