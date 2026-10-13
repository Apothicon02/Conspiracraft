package org.conspiracraft.space;

import org.conspiracraft.graphics.buffers.Buffer;
import org.conspiracraft.graphics.textures.ImageHelper;
import org.conspiracraft.graphics.textures.Textures;
import org.conspiracraft.utils.Utils;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class Celestials {
    public static int celestialTexWidth = 16;
    public static int celestialTexHeight = celestialTexWidth*6;
    public static Map<Integer, CelestialType> celestialTypeeMap = new HashMap<>(Map.of());

    public static int getId(CelestialType type) {
        int id = 0;
        for (CelestialType mapBlocKType : celestialTypeeMap.values()) {
            if (mapBlocKType.equals(type)) {
                return id;
            }
            id++;
        }
        return 0;
    }

    public static CelestialType
            MUN = create(new CelestialType("texture/mun")),
            DYNAMO = create(new CelestialType("texture/dynamo")),
            OLIVIUS = create(new CelestialType("texture/olivius")),
            OLIVIUS_CLOUDS = create(new CelestialType("texture/olivius_clouds")),
            MARB = create(new CelestialType("texture/marb")),
            VERA = create(new CelestialType("texture/vera")),
            VERA_CLOUDS = create(new CelestialType("texture/vera_clouds")),
            VERA_ORANGE_CLOUDS = create(new CelestialType("texture/vera_orange_clouds")),
            AKSALA = create(new CelestialType("texture/aksala")),
            AKSALA_AURORAS = create(new CelestialType("texture/aksala_auroras")),
            SUN = create(new CelestialType("texture/sun")),
            LAZULI = create(new CelestialType("texture/lazuli")),
            EARTH = create(new CelestialType("texture/earth")),
            EARTH_CLOUDS = create(new CelestialType("texture/earth_clouds"));

    private static CelestialType create(CelestialType type) {
        celestialTypeeMap.put(celestialTypeeMap.size(), type);
        return type;
    }
    public static void fillTexture(MemoryStack stack) throws IOException {
        int texSize = Textures.celestials.width*Textures.celestials.height;
        Buffer stagingBuffer = new Buffer(stack, texSize*4, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, true);

        int xOffset = 0;
        int yOffset = 0;
        for (CelestialType celestialTypee : celestialTypeeMap.values()) {
            ByteBuffer celestialBuf = Utils.imageToBuffer(Utils.loadImage("celestial/"+celestialTypee.name));
            for (long row = 0; row < celestialTexHeight; row++) {
                memCopy(memAddress(celestialBuf) + row * celestialTexWidth * 4L,
                        stagingBuffer.pointer.get(0) + (((yOffset + row) * Textures.celestials.width + xOffset) * 4L),
                        celestialTexWidth * 4L);
            }
            memFree(celestialBuf);
            celestialTypee.atlasOffset(xOffset, yOffset);
            xOffset += celestialTexWidth;
            if (xOffset + celestialTexWidth > Textures.celestials.width) {
                xOffset = 0;
                yOffset += celestialTexHeight;
            }
        }
        ImageHelper.fillImage(stack, Textures.celestials, stagingBuffer);
    }
}
