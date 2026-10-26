package org.conspiracraft.blocks.types;

import org.conspiracraft.blocks.Material;

import java.util.Map;

public class LightBlockType extends BlockType {
    public LightBlockProperties lightBlockProperties() {
        return (LightBlockProperties) blockProperties;
    }

    public LightBlockType(String name, Map<Integer, Material> materials, LightBlockProperties blockProperties) {super(name, materials, blockProperties);}
    public LightBlockType(String name, LightBlockProperties blockProperties) {super(name, blockProperties);}
}
