package org.conspiracraft.game.blocks.types;

public class LightBlockType extends BlockType {
    public LightBlockProperties lightBlockProperties() {
        return (LightBlockProperties) blockProperties;
    }

    public LightBlockType(LightBlockProperties blockProperties) {
        super(blockProperties);
    }
}
