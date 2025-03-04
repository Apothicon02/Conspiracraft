package org.conspiracraft.game.blocks.types;

import org.joml.Vector2i;

public class LeafBlockType extends BlockType {
    public boolean obstructingHeightmap(Vector2i block) {
        return this.obstructsHeightmap ? true : block.y > 0;
    }

    public LeafBlockType(boolean solid, boolean canBlockLight, boolean collidable, boolean fluidReplacable, boolean fluid, boolean obstructHeightmap) {
        super(solid, canBlockLight, collidable, fluidReplacable, fluid, obstructHeightmap);
    }
    public LeafBlockType(boolean solid, boolean canBlockLight, boolean collidable, boolean fluidReplacable, boolean fluid) {
        super(solid, canBlockLight, collidable, fluidReplacable, fluid);
    }
    public LeafBlockType(boolean solid, boolean canBlockLight, boolean collidable, boolean fluidReplacable) {
        super(solid, canBlockLight, collidable, fluidReplacable);
    }
    public LeafBlockType(boolean solid, boolean canBlockLight, boolean collidable) {
        super(solid, canBlockLight, collidable);
    }
    public LeafBlockType(boolean solid, boolean canBlockLight) {
        super(solid, canBlockLight);
    }
    public LeafBlockType(boolean solid) {
        super(solid);
    }
    public LeafBlockType() {
        super();
    }
}
