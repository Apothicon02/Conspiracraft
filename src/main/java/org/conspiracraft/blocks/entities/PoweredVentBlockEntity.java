package org.conspiracraft.blocks.entities;

import org.conspiracraft.utils.Utils;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.conspiracraft.Main;
import org.conspiracraft.audio.Sounds;
import org.conspiracraft.audio.Source;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.World;

public class PoweredVentBlockEntity extends BlockEntity {
    public PoweredVentBlockEntity() {

    }

    @Override
    public BlockEntity create() {
        return new PoweredVentBlockEntity();
    }

    Source source = null;

    @Override
    public void remove(int xyz) {
        if (source != null) {
            source.delete();
            source = null;
        }
        World.blockEntities.remove(xyz);
    }

    @Override
    public void tick(Vector2i block, Vector3i pos) {
        boolean inAudibleRange = pos.distance(new Vector3i((int) Main.player.pos.x(), (int) (Main.player.pos.y()+Main.player.eyeHeight), (int) Main.player.pos.z())) < 50;
        if (inAudibleRange) {
            if (source == null) {
                source = new Source(new Vector3f(pos.x() + 0.5f, pos.y() + 0.5f, pos.z() + 0.5f), 1.f, 1.1f+Utils.randomFloat(0.2f), 0.f, 1); //random pitch offset is to prevent audio summing
                source.play(Sounds.VENT);
            }
        } else if (source != null) {
            source.delete();
            source = null;
        }
    }
}
