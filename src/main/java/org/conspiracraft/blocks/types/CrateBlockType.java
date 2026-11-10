package org.conspiracraft.blocks.types;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.Material;
import org.conspiracraft.gui.GUI;

import java.util.Map;

public class CrateBlockType extends BlockType {
	public CrateBlockType(String name, Map<Integer, Material> materials, BlockProperties blockProperties) {
        super(name, materials, blockProperties);
    }

    @Override
    public int use(int x, int y, int z, int block) {
        GUI.inventoryOpen = true;
        GUI.menus.add(Main.player.inv.containerMenu);
        return 200;
    }
}
