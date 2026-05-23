package org.conspiracraft.blocks.types;

public class LightBlockType extends BlockType {
    public LightBlockProperties lightBlockProperties() {
        return (LightBlockProperties) blockProperties;
    }

    public LightBlockType(int id, String name, LightBlockProperties blockProperties) {super(id, name, blockProperties);}
}
