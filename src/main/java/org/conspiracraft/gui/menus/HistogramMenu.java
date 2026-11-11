package org.conspiracraft.gui.menus;

import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.gui.*;
import org.conspiracraft.items.types.ItemTypes;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.Chunk;
import org.conspiracraft.world.Region;
import org.conspiracraft.world.World;
import org.joml.Vector2i;
import org.joml.Vector4f;

import java.util.Arrays;

import static org.conspiracraft.graphics.Renderer.pushUBO;
import static org.conspiracraft.gui.GUI.*;

public class HistogramMenu extends Menu {
    int rowHeight = 10;
    final float[] lengths = new float[10];
    final int[] amounts = new int[10];
    public HistogramMenu() {
        this.menuSizeRaw = new Vector2i(GUI.slotSize*8, (rowHeight*10)+(hideName ? 0 : 12));
        init();
    }

    public static final Vector4f[] colors = new Vector4f[]{
            new Vector4f(0, 0, 0, 1), new Vector4f(0.34f, 0, 1, 1), new Vector4f(0, 0, 1, 1), new Vector4f(0, 1, 0.75f, 1),
            new Vector4f(0.25f, 1, 0, 1), new Vector4f(1, 1, 0, 1), new Vector4f(1, 0.5f, 0, 1), new Vector4f(1, 0, 0, 1),
            new Vector4f(1, 0, 1, 1), new Vector4f(1, 1, 1, 1)
    };
    public static final char[][] labels = new char[][]{"<=1".toCharArray(), "2".toCharArray(), "3".toCharArray(), "4".toCharArray(), "5".toCharArray(), "6".toCharArray(), "7".toCharArray(), "8".toCharArray(), "9".toCharArray(), ">=10".toCharArray()};

    @Override
    public void draw() {
        drawBase();

        pushUBO.updateTex(null);
        color.set(0.329f, 0.333f, 0.341f, 0.88f);
        drawQuad(0, 0, menuSizeRaw.x(), menuSizeRaw.y()-rowHeight, 1);
        for (int i = 0; i < 10; i++) {
            pushUBO.updateTex(null);
            color.set(colors[i]);
            double barLength = Math.sqrt(lengths[i]);
            drawQuad(0, rowHeight*i, (int) (menuSizeRaw.x()*barLength), rowHeight, 1);

            boolean invert = color.x() > 0.8f && color.y() > 0.8f && color.z() > 0.8f;

            color.set(1);
            pushUBO.updateTex(Textures.gui);
            pushUBO.updateLayer(0);
            drawText(false, 0, 0, 0, rowHeight*(i+0.1f), labels[i], 1, invert && barLength > 0.1f);
            char[] chars = (Utils.shortenHugeNumber(amounts[i])+" chunks").toCharArray();
            drawText(false, (int)(((menuSizeRaw.x()-(chars.length*charWidth))*0.5f)-2), 0, 0, rowHeight*(i+0.1f), chars, 1, invert && barLength > 0.5f);
            chars = (String.format("%.2f", lengths[i]*100)+"%").toCharArray();
            drawText(false, (menuSizeRaw.x()-(chars.length*charWidth)-2), 0, 0, rowHeight*(i+0.1f), chars, 1, invert && barLength > 0.9f);
        }
    }

    @Override
    public void tick() {
        baseTick();
        Arrays.fill(lengths, 0);
        int counted = 0;
        for (Region region : World.getAllRegions()) {
            if (region != null) {
                for (Chunk chunk : region.chunks) {
                    //if (chunk.blockPalette.size() > 1) {
                    int i = Math.clamp(chunk.blockPalette.size() - 1, 0, 9);
                    lengths[i]++;
                    counted++;
                    //}
                }
            }
        }
        for (int i = 0; i < lengths.length; i++) {
            amounts[i] = (int)lengths[i];
            lengths[i] = lengths[i]/counted;
        }
    }
}